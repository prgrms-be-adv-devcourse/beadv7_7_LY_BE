package site.fulfillmentservice.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import site.fulfillmentservice.settlement.domain.Money;
import site.fulfillmentservice.settlement.domain.SettlementBatch;
import site.fulfillmentservice.settlement.domain.SettlementBatchRepository;
import site.fulfillmentservice.settlement.domain.SettlementItem;
import site.fulfillmentservice.settlement.domain.SettlementItemRepository;
import site.fulfillmentservice.settlement.domain.SettlementStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementBatchService")
class SettlementBatchServiceTest {

    private static final Long SELLER_ID = 302L;

    @Mock
    private SettlementItemRepository settlementItemRepository;

    @Mock
    private SettlementBatchRepository settlementBatchRepository;

    @Mock
    private SettlementEventPublisher settlementEventPublisher;

    @InjectMocks
    private SettlementBatchService settlementBatchService;

    @Captor
    private ArgumentCaptor<SettlementBatch> settlementBatchCaptor;

    private static SettlementItem settlementItem(Long orderId, Long sellerId, long finalBidPrice) {
        return SettlementItem.of(orderId, sellerId, Money.of(finalBidPrice), BigDecimal.valueOf(0.1000), LocalDateTime.now());
    }

    @Nested
    @DisplayName("findEligibleSellerIds")
    class FindEligibleSellerIds {

        @Test
        @DisplayName("PENDING 항목이 있는 판매자 id를 중복 없이 반환한다")
        void returnsDistinctSellerIds() {
            // given
            LocalDateTime periodTo = LocalDateTime.now();
            given(settlementItemRepository.findDistinctSellerIdsByStatusAndCompletedAtBefore(SettlementStatus.PENDING, periodTo))
                    .willReturn(List.of(301L, 302L));

            // when
            List<Long> sellerIds = settlementBatchService.findEligibleSellerIds(periodTo);

            // then
            assertThat(sellerIds).containsExactlyInAnyOrder(301L, 302L);
        }

        @Test
        @DisplayName("대상이 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoneEligible() {
            // given
            LocalDateTime periodTo = LocalDateTime.now();
            given(settlementItemRepository.findDistinctSellerIdsByStatusAndCompletedAtBefore(SettlementStatus.PENDING, periodTo))
                    .willReturn(List.of());

            // when
            List<Long> sellerIds = settlementBatchService.findEligibleSellerIds(periodTo);

            // then
            assertThat(sellerIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("createBatchForSeller")
    class CreateBatchForSeller {

        private final LocalDateTime periodFrom = LocalDateTime.now().minusMonths(1);
        private final LocalDateTime periodTo = LocalDateTime.now();
        private final LocalDateTime confirmedAt = LocalDateTime.now();

        @Test
        @DisplayName("PENDING 항목들을 모아 배치를 생성하고, 항목들을 CONFIRMED로 전환하며 정산 확정 이벤트를 발행한다")
        void createsBatchMarksItemsConfirmedAndPublishesEvent() {
            // given
            SettlementItem item1 = settlementItem(1001L, SELLER_ID, 85_000);
            SettlementItem item2 = settlementItem(1002L, SELLER_ID, 15_000);
            given(settlementBatchRepository.existsBySellerIdAndPeriodFromAndPeriodTo(SELLER_ID, periodFrom, periodTo))
                    .willReturn(false);
            given(settlementItemRepository.findAllByStatusAndCompletedAtBeforeAndSellerId(
                    SettlementStatus.PENDING, periodTo, SELLER_ID))
                    .willReturn(List.of(item1, item2));
            given(settlementBatchRepository.save(settlementBatchCaptor.capture()))
                    .willAnswer(invocation -> {
                        SettlementBatch batch = invocation.getArgument(0);
                        ReflectionTestUtils.setField(batch, "id", 9001L);
                        return batch;
                    });

            // when
            settlementBatchService.createBatchForSeller(SELLER_ID, periodFrom, periodTo, confirmedAt);

            // then
            SettlementBatch savedBatch = settlementBatchCaptor.getValue();
            assertThat(savedBatch.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(item1.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
            assertThat(item1.getSettlementBatchId()).isEqualTo(9001L);
            assertThat(item1.getConfirmedAt()).isEqualTo(confirmedAt);
            assertThat(item2.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
            assertThat(item2.getSettlementBatchId()).isEqualTo(9001L);
            verify(settlementEventPublisher).publishConfirmed(savedBatch);
        }

        @Test
        @DisplayName("이미 같은 기간에 배치가 생성된 판매자면 건너뛴다")
        void skipsWhenBatchAlreadyExistsForPeriod() {
            // given
            given(settlementBatchRepository.existsBySellerIdAndPeriodFromAndPeriodTo(SELLER_ID, periodFrom, periodTo))
                    .willReturn(true);

            // when
            settlementBatchService.createBatchForSeller(SELLER_ID, periodFrom, periodTo, confirmedAt);

            // then
            verify(settlementItemRepository, never())
                    .findAllByStatusAndCompletedAtBeforeAndSellerId(any(), any(), any());
            verify(settlementBatchRepository, never()).save(any());
            verify(settlementEventPublisher, never()).publishConfirmed(any());
        }

        @Test
        @DisplayName("대상 PENDING 항목이 없으면 배치를 생성하지 않는다")
        void skipsWhenNoPendingItems() {
            // given
            given(settlementBatchRepository.existsBySellerIdAndPeriodFromAndPeriodTo(SELLER_ID, periodFrom, periodTo))
                    .willReturn(false);
            given(settlementItemRepository.findAllByStatusAndCompletedAtBeforeAndSellerId(
                    SettlementStatus.PENDING, periodTo, SELLER_ID))
                    .willReturn(List.of());

            // when
            settlementBatchService.createBatchForSeller(SELLER_ID, periodFrom, periodTo, confirmedAt);

            // then
            verify(settlementBatchRepository, never()).save(any());
            verify(settlementEventPublisher, never()).publishConfirmed(any());
        }

        @Test
        @DisplayName("저장 중 동시성으로 유니크 제약을 위반하면 항목 전환/이벤트 발행 없이 종료한다")
        void skipsMarkPaidAndEventWhenConcurrentDuplicateSave() {
            // given
            SettlementItem item = settlementItem(1001L, SELLER_ID, 85_000);
            given(settlementBatchRepository.existsBySellerIdAndPeriodFromAndPeriodTo(SELLER_ID, periodFrom, periodTo))
                    .willReturn(false);
            given(settlementItemRepository.findAllByStatusAndCompletedAtBeforeAndSellerId(
                    SettlementStatus.PENDING, periodTo, SELLER_ID))
                    .willReturn(List.of(item));
            given(settlementBatchRepository.save(any()))
                    .willThrow(new DataIntegrityViolationException("duplicate seller/period"));

            // when
            settlementBatchService.createBatchForSeller(SELLER_ID, periodFrom, periodTo, confirmedAt);

            // then
            assertThat(item.getStatus()).isEqualTo(SettlementStatus.PENDING);
            verify(settlementEventPublisher, never()).publishConfirmed(any());
        }
    }
}
