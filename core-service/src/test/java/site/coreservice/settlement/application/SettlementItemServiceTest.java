package site.coreservice.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
import site.common.event.contract.OrderCompletedEvent;
import site.coreservice.settlement.domain.CommissionPolicy;
import site.coreservice.settlement.domain.CommissionPolicyRepository;
import site.coreservice.settlement.domain.SettlementItem;
import site.coreservice.settlement.domain.SettlementItemRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementItemService")
class SettlementItemServiceTest {

    @Mock
    private SettlementItemRepository settlementItemRepository;

    @Mock
    private CommissionPolicyRepository commissionPolicyRepository;

    @InjectMocks
    private SettlementItemService settlementItemService;

    @Captor
    private ArgumentCaptor<SettlementItem> settlementItemCaptor;

    private OrderCompletedEvent orderCompletedEvent;

    @BeforeEach
    void setUp() {
        orderCompletedEvent = OrderCompletedEvent.builder()
                .orderId(1001L)
                .auctionId(5001L)
                .buyerId(301L)
                .sellerId(302L)
                .finalBidPrice(BigDecimal.valueOf(85_000))
                .completedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createSettlementItem")
    class CreateSettlementItem {

        @Test
        @DisplayName("유효한 수수료 정책이 있으면 그 rate로 정산 항목을 생성한다")
        void createsSettlementItemWithPolicyRate() {
            // given
            CommissionPolicy policy = CommissionPolicy.of(
                    BigDecimal.valueOf(0.1000), LocalDateTime.now().minusDays(1), null);
            given(settlementItemRepository.existsByOrderId(1001L)).willReturn(false);
            given(commissionPolicyRepository.findByEffectiveToIsNull()).willReturn(Optional.of(policy));
            given(settlementItemRepository.save(settlementItemCaptor.capture()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            settlementItemService.createSettlementItem(orderCompletedEvent);

            // then
            SettlementItem saved = settlementItemCaptor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(1001L);
            assertThat(saved.getSellerId()).isEqualTo(302L);
            assertThat(saved.getFinalBidPrice().getValue()).isEqualByComparingTo(BigDecimal.valueOf(85_000));
            assertThat(saved.getCommissionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.1000));
            assertThat(saved.getCommissionAmount().getValue()).isEqualByComparingTo(BigDecimal.valueOf(8_500));
            assertThat(saved.getNetAmount().getValue()).isEqualByComparingTo(BigDecimal.valueOf(76_500));
        }

        @Test
        @DisplayName("유효한 정책이 없으면 기본 수수료율(5%)을 사용한다")
        void fallsBackToDefaultRateWhenNoPolicy() {
            // given
            given(settlementItemRepository.existsByOrderId(1001L)).willReturn(false);
            given(commissionPolicyRepository.findByEffectiveToIsNull()).willReturn(Optional.empty());
            given(settlementItemRepository.save(settlementItemCaptor.capture()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            settlementItemService.createSettlementItem(orderCompletedEvent);

            // then
            assertThat(settlementItemCaptor.getValue().getCommissionRate())
                    .isEqualByComparingTo(BigDecimal.valueOf(0.0500));
        }

        @Test
        @DisplayName("정책은 있지만 완료 시각에 유효하지 않으면 기본 수수료율을 사용한다")
        void fallsBackToDefaultRateWhenPolicyNotEffective() {
            // given
            CommissionPolicy futurePolicy = CommissionPolicy.of(
                    BigDecimal.valueOf(0.1000), LocalDateTime.now().plusDays(1), null);
            given(settlementItemRepository.existsByOrderId(1001L)).willReturn(false);
            given(commissionPolicyRepository.findByEffectiveToIsNull()).willReturn(Optional.of(futurePolicy));
            given(settlementItemRepository.save(settlementItemCaptor.capture()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            settlementItemService.createSettlementItem(orderCompletedEvent);

            // then
            assertThat(settlementItemCaptor.getValue().getCommissionRate())
                    .isEqualByComparingTo(BigDecimal.valueOf(0.0500));
        }

        @Test
        @DisplayName("이미 같은 orderId로 정산 항목이 생성되어 있으면 중복 생성을 건너뛴다")
        void skipsWhenSettlementItemAlreadyExists() {
            // given
            given(settlementItemRepository.existsByOrderId(1001L)).willReturn(true);

            // when
            settlementItemService.createSettlementItem(orderCompletedEvent);

            // then
            verify(commissionPolicyRepository, never()).findByEffectiveToIsNull();
            verify(settlementItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("저장 중 동시성으로 유니크 제약을 위반해도 예외를 던지지 않는다")
        void swallowsConcurrentDuplicateSave() {
            // given
            given(settlementItemRepository.existsByOrderId(1001L)).willReturn(false);
            given(commissionPolicyRepository.findByEffectiveToIsNull()).willReturn(Optional.empty());
            given(settlementItemRepository.save(any()))
                    .willThrow(new DataIntegrityViolationException("duplicate order_id"));

            // when & then (예외 없이 정상 종료되어야 함)
            settlementItemService.createSettlementItem(orderCompletedEvent);
        }
    }
}
