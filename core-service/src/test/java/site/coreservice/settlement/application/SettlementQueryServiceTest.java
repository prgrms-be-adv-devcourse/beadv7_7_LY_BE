package site.coreservice.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.settlement.application.dto.SettlementBatchResult;
import site.coreservice.settlement.application.dto.SettlementItemSearchResult;
import site.coreservice.settlement.domain.Money;
import site.coreservice.settlement.domain.SettlementBatch;
import site.coreservice.settlement.domain.SettlementBatchRepository;
import site.coreservice.settlement.domain.SettlementItem;
import site.coreservice.settlement.domain.SettlementItemRepository;
import site.coreservice.settlement.domain.SettlementItemSearchPage;
import site.coreservice.settlement.domain.SettlementStatus;
import site.coreservice.settlement.exception.SettlementException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementQueryService")
class SettlementQueryServiceTest {

    private static final Long SELLER_ID = 302L;

    @Mock
    private SettlementItemRepository settlementItemRepository;

    @Mock
    private SettlementBatchRepository settlementBatchRepository;

    @InjectMocks
    private SettlementQueryService settlementQueryService;

    private static SettlementItem settlementItem(Long orderId, Long sellerId, long finalBidPrice) {
        return SettlementItem.of(orderId, sellerId, Money.of(finalBidPrice), BigDecimal.valueOf(0.1000), LocalDateTime.now());
    }

    @Nested
    @DisplayName("findItems")
    class FindItems {

        @Test
        @DisplayName("status/from/to 없이 조회하면 기본 page/size로 검색하고 결과를 변환한다")
        void searchesWithDefaultsWhenNoFilters() {
            // given
            SettlementItem item = settlementItem(1001L, SELLER_ID, 85_000);
            SettlementItemSearchPage searchPage = new SettlementItemSearchPage(List.of(item), 1L);
            given(settlementItemRepository.search(SELLER_ID, null, null, null, 0, 20))
                    .willReturn(searchPage);

            // when
            SettlementItemSearchResult result = settlementQueryService.findItems(SELLER_ID, null, null, null, 0, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).orderId()).isEqualTo(1001L);
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("status가 유효한 값이면 파싱해서 검색 조건으로 넘긴다")
        void parsesValidStatus() {
            // given
            given(settlementItemRepository.search(SELLER_ID, SettlementStatus.CONFIRMED, null, null, 0, 20))
                    .willReturn(new SettlementItemSearchPage(List.of(), 0L));

            // when
            settlementQueryService.findItems(SELLER_ID, "CONFIRMED", null, null, 0, 20);

            // then
            verify(settlementItemRepository).search(SELLER_ID, SettlementStatus.CONFIRMED, null, null, 0, 20);
        }

        @Test
        @DisplayName("status가 유효하지 않으면 예외가 발생하고 검색을 실행하지 않는다")
        void invalidStatus_throwsExceptionWithoutSearching() {
            // when & then
            assertThatThrownBy(() -> settlementQueryService.findItems(SELLER_ID, "INVALID", null, null, 0, 20))
                    .isInstanceOf(SettlementException.class)
                    .hasMessage("유효하지 않은 정산 상태입니다.");

            verify(settlementItemRepository, never()).search(any(), any(), any(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("from이 to보다 늦으면 예외가 발생하고 검색을 실행하지 않는다")
        void fromAfterTo_throwsExceptionWithoutSearching() {
            // given
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.plusDays(1);

            // when & then
            assertThatThrownBy(() -> settlementQueryService.findItems(SELLER_ID, null, from, to, 0, 20))
                    .isInstanceOf(SettlementException.class)
                    .hasMessage("from은 to보다 늦을 수 없습니다.");

            verify(settlementItemRepository, never()).search(any(), any(), any(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("page가 음수면 0으로 보정한다")
        void clampsNegativePageToZero() {
            // given
            given(settlementItemRepository.search(eq(SELLER_ID), isNull(), isNull(), isNull(), eq(0), eq(20)))
                    .willReturn(new SettlementItemSearchPage(List.of(), 0L));

            // when
            settlementQueryService.findItems(SELLER_ID, null, null, null, -1, 20);

            // then
            verify(settlementItemRepository).search(eq(SELLER_ID), isNull(), isNull(), isNull(), eq(0), eq(20));
        }

        @Test
        @DisplayName("size가 1 미만이면 기본값(20)으로, 100 초과면 100으로 보정한다")
        void clampsSize() {
            // given
            given(settlementItemRepository.search(eq(SELLER_ID), isNull(), isNull(), isNull(), eq(0), eq(20)))
                    .willReturn(new SettlementItemSearchPage(List.of(), 0L));
            given(settlementItemRepository.search(eq(SELLER_ID), isNull(), isNull(), isNull(), eq(0), eq(100)))
                    .willReturn(new SettlementItemSearchPage(List.of(), 0L));

            // when
            settlementQueryService.findItems(SELLER_ID, null, null, null, 0, 0);
            settlementQueryService.findItems(SELLER_ID, null, null, null, 0, 500);

            // then
            verify(settlementItemRepository).search(eq(SELLER_ID), isNull(), isNull(), isNull(), eq(0), eq(20));
            verify(settlementItemRepository).search(eq(SELLER_ID), isNull(), isNull(), isNull(), eq(0), eq(100));
        }
    }

    @Nested
    @DisplayName("findBatches")
    class FindBatches {

        @Test
        @DisplayName("판매자의 정산 배치 목록을 결과로 변환한다")
        void returnsSellerBatches() {
            // given
            List<SettlementItem> items = List.of(settlementItem(1001L, SELLER_ID, 85_000));
            SettlementBatch batch = SettlementBatch.of(SELLER_ID, items,
                    LocalDateTime.now().minusMonths(1), LocalDateTime.now(), LocalDateTime.now());
            given(settlementBatchRepository.findAllBySellerId(SELLER_ID)).willReturn(List.of(batch));

            // when
            List<SettlementBatchResult> results = settlementQueryService.findBatches(SELLER_ID);

            // then
            assertThat(results).hasSize(1);
            // totalAmount는 netAmount(finalBidPrice - commissionAmount) 합계: 85_000 - 8_500 = 76_500
            assertThat(results.get(0).totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(76_500));
        }

        @Test
        @DisplayName("대상이 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoBatches() {
            // given
            given(settlementBatchRepository.findAllBySellerId(SELLER_ID)).willReturn(List.of());

            // when
            List<SettlementBatchResult> results = settlementQueryService.findBatches(SELLER_ID);

            // then
            assertThat(results).isEmpty();
        }
    }
}
