package site.coreservice.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SettlementBatch")
class SettlementBatchTest {

    private static final Long SELLER_ID = 302L;

    private static SettlementItem settlementItem(Long orderId, Long sellerId, long finalBidPrice) {
        return SettlementItem.of(orderId, sellerId, Money.of(finalBidPrice), BigDecimal.valueOf(0.1000), LocalDateTime.now());
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("생성 시 전달한 값들이 그대로 저장된다")
        void createStoresGivenValues() {
            // given
            LocalDateTime periodFrom = LocalDateTime.now().minusDays(7);
            LocalDateTime periodTo = LocalDateTime.now();
            LocalDateTime confirmedAt = LocalDateTime.now();
            List<SettlementItem> items = List.of(settlementItem(5001L, SELLER_ID, 85_000));

            // when
            SettlementBatch batch = SettlementBatch.of(SELLER_ID, items, periodFrom, periodTo, confirmedAt);

            // then
            assertThat(batch.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(batch.getPeriodFrom()).isEqualTo(periodFrom);
            assertThat(batch.getPeriodTo()).isEqualTo(periodTo);
            assertThat(batch.getConfirmedAt()).isEqualTo(confirmedAt);
        }

        @Test
        @DisplayName("여러 정산 항목의 netAmount를 합산해 totalAmount를 계산한다")
        void sumsNetAmountsFromItems() {
            // given: (85_000 - 8_500) + (50_000 - 5_000) = 76_500 + 45_000 = 121_500
            List<SettlementItem> items = List.of(
                    settlementItem(5001L, SELLER_ID, 85_000),
                    settlementItem(5002L, SELLER_ID, 50_000));

            // when
            SettlementBatch batch = SettlementBatch.of(SELLER_ID, items,
                    LocalDateTime.now().minusDays(7), LocalDateTime.now(), LocalDateTime.now());

            // then
            assertThat(batch.getTotalAmount()).isEqualTo(Money.of(121_500));
        }

        @Test
        @DisplayName("정산 항목이 비어있으면 예외가 발생한다")
        void emptyItems_throwsException() {
            // given & when & then
            assertThatThrownBy(() -> SettlementBatch.of(SELLER_ID, List.of(),
                    LocalDateTime.now().minusDays(7), LocalDateTime.now(), LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("정산 항목이 하나 이상 있어야 합니다.");
        }

        @Test
        @DisplayName("sellerId와 다른 판매자의 항목이 섞여 있으면 예외가 발생한다")
        void mismatchedSellerItem_throwsException() {
            // given
            List<SettlementItem> items = List.of(
                    settlementItem(5001L, SELLER_ID, 85_000),
                    settlementItem(5002L, 999L, 50_000));

            // when & then
            assertThatThrownBy(() -> SettlementBatch.of(SELLER_ID, items,
                    LocalDateTime.now().minusDays(7), LocalDateTime.now(), LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("모든 정산 항목의 판매자가 sellerId와 일치해야 합니다.");
        }

        @Test
        @DisplayName("periodFrom이 periodTo보다 늦으면 예외가 발생한다")
        void periodFromAfterPeriodTo_throwsException() {
            // given
            LocalDateTime periodFrom = LocalDateTime.now();
            LocalDateTime periodTo = periodFrom.minusDays(1);
            List<SettlementItem> items = List.of(settlementItem(5001L, SELLER_ID, 85_000));

            // when & then
            assertThatThrownBy(() -> SettlementBatch.of(SELLER_ID, items, periodFrom, periodTo, LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("periodFrom은 periodTo보다 이전이어야 합니다.");
        }

        @Test
        @DisplayName("periodFrom과 periodTo가 같으면 예외가 발생한다")
        void periodFromEqualsPeriodTo_throwsException() {
            // given
            LocalDateTime period = LocalDateTime.now();
            List<SettlementItem> items = List.of(settlementItem(5001L, SELLER_ID, 85_000));

            // when & then
            assertThatThrownBy(() -> SettlementBatch.of(SELLER_ID, items, period, period, LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
