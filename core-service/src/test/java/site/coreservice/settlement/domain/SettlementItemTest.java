package site.coreservice.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SettlementItem")
class SettlementItemTest {

    private static SettlementItem defaultItem() {
        return SettlementItem.of(5001L, 302L, Money.of(85_000), BigDecimal.valueOf(0.1000),
                Money.of(8_500), Money.of(76_500), LocalDateTime.now());
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
            SettlementItem item = SettlementItem.of(5001L, 302L, Money.of(85_000), BigDecimal.valueOf(0.1000),
                    Money.of(8_500), Money.of(76_500), completedAt);

            // then
            assertThat(item.getOrderId()).isEqualTo(5001L);
            assertThat(item.getSellerId()).isEqualTo(302L);
            assertThat(item.getFinalBidPrice()).isEqualTo(Money.of(85_000));
            assertThat(item.getCommissionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.1000));
            assertThat(item.getCommissionAmount()).isEqualTo(Money.of(8_500));
            assertThat(item.getNetAmount()).isEqualTo(Money.of(76_500));
            assertThat(item.getCompletedAt()).isEqualTo(completedAt);
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
}
