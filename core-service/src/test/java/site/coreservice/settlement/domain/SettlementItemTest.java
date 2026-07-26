package site.coreservice.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SettlementItem")
class SettlementItemTest {

    private static SettlementItem defaultItem() {
        return SettlementItem.of(5001L, 302L, Money.of(85_000), BigDecimal.valueOf(0.1000), LocalDateTime.now());
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("생성하면 PENDING 상태로 시작한다")
        void createStartsAsPending() {
            // given & when
            SettlementItem item = defaultItem();

            // then
            assertThat(item.getStatus()).isEqualTo(SettlementStatus.PENDING);
        }

        @Test
        @DisplayName("생성 시 전달한 값들이 그대로 저장된다")
        void createStoresGivenValues() {
            // given
            LocalDateTime completedAt = LocalDateTime.now();

            // when
            SettlementItem item = SettlementItem.of(5001L, 302L, Money.of(85_000), BigDecimal.valueOf(0.1000), completedAt);

            // then
            assertThat(item.getOrderId()).isEqualTo(5001L);
            assertThat(item.getSellerId()).isEqualTo(302L);
            assertThat(item.getFinalBidPrice()).isEqualTo(Money.of(85_000));
            assertThat(item.getCommissionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.1000));
            assertThat(item.getCompletedAt()).isEqualTo(completedAt);
        }

        @Test
        @DisplayName("commissionAmount와 netAmount는 finalBidPrice와 commissionRate로부터 자동 계산된다")
        void computesCommissionAndNetAmount() {
            // given & when
            SettlementItem item = SettlementItem.of(5001L, 302L, Money.of(85_000), BigDecimal.valueOf(0.1000),
                    LocalDateTime.now());

            // then
            assertThat(item.getCommissionAmount()).isEqualTo(Money.of(8_500));
            assertThat(item.getNetAmount()).isEqualTo(Money.of(76_500));
        }

        @Test
        @DisplayName("곱셈 결과에 소수점이 생기면 버림 처리된 commissionAmount로 netAmount가 계산된다")
        void computesWithTruncatedCommission() {
            // given & when (999 * 0.1 = 99.9 -> 버림 처리되어 99)
            SettlementItem item = SettlementItem.of(5001L, 302L, Money.of(999), BigDecimal.valueOf(0.1), LocalDateTime.now());

            // then
            assertThat(item.getCommissionAmount()).isEqualTo(Money.of(99));
            assertThat(item.getNetAmount()).isEqualTo(Money.of(900));
        }

        @Test
        @DisplayName("생성 시점에는 지급/배치 관련 값이 비어 있다")
        void createLeavesSettlementFieldsEmpty() {
            // given & when
            SettlementItem item = defaultItem();

            // then
            assertThat(item.getPaidAt()).isNull();
            assertThat(item.getSettlementBatchId()).isNull();
        }
    }

    @Nested
    @DisplayName("지급 처리 (markPaid)")
    class MarkPaid {

        @Test
        @DisplayName("PENDING 상태에서 지급 처리하면 PAID로 바뀌고 배치 정보가 채워진다")
        void markPaidFromPending() {
            // given
            SettlementItem item = defaultItem();
            LocalDateTime paidAt = LocalDateTime.now();

            // when
            item.markPaid(9001L, paidAt);

            // then
            assertThat(item.getStatus()).isEqualTo(SettlementStatus.PAID);
            assertThat(item.getSettlementBatchId()).isEqualTo(9001L);
            assertThat(item.getPaidAt()).isEqualTo(paidAt);
        }

        @Test
        @DisplayName("이미 지급 처리된 항목을 다시 지급 처리하려 하면 예외가 발생한다")
        void markPaidAlreadyPaid_throwsException() {
            // given
            SettlementItem item = defaultItem();
            item.markPaid(9001L, LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> item.markPaid(9002L, LocalDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("PENDING 상태의 정산 항목만 지급 처리할 수 있습니다.");
        }
    }
}
