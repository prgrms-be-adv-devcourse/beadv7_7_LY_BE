package site.coreservice.product.domain.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PriceHistoryTest {

    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 7, 10, 20, 31);
    private static final LocalDateTime CONFIRMED_AT = LocalDateTime.of(2026, 7, 11, 10, 0);

    private ClosedAuction closedAuction(Long finalPrice, Integer bidCount) {
        return new ClosedAuction(1024L, 55L, MediaCondition.NEAR_MINT, finalPrice, bidCount, CLOSED_AT, "ENDED_WON");
    }

    @Test
    @DisplayName("팩토리는 경매 정보와 확정시각을 각 필드에 매핑하고 이상치 플래그는 false로 둔다")
    void of_필드_매핑과_이상치_기본값() {
        // given-when
        PriceHistory priceHistory = PriceHistory.of(closedAuction(72000L, 7), CONFIRMED_AT);

        // then
        assertThat(priceHistory.getAuctionId()).isEqualTo(1024L);
        assertThat(priceHistory.getProductId()).isEqualTo(55L);
        assertThat(priceHistory.getMediaCondition()).isEqualTo(MediaCondition.NEAR_MINT);
        assertThat(priceHistory.getFinalPrice()).isEqualTo(72000L);
        assertThat(priceHistory.getBidCount()).isEqualTo(7);
        assertThat(priceHistory.getTradedAt()).isEqualTo(CLOSED_AT);
        assertThat(priceHistory.getConfirmedAt()).isEqualTo(CONFIRMED_AT);
        assertThat(priceHistory.isOutlier()).isFalse();
        assertThat(priceHistory.getExclusionReason()).isNull();
    }

    @Test
    @DisplayName("낙찰가는 0이나 음수를 거부하고 1원은 허용한다")
    void of_낙찰가_경계_검증() {
        // given-when-then
        assertThatThrownBy(() -> PriceHistory.of(closedAuction(0L, 7), CONFIRMED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PriceHistory.of(closedAuction(-1000L, 7), CONFIRMED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PriceHistory.of(closedAuction(1L, 7), CONFIRMED_AT).getFinalPrice()).isEqualTo(1L);
    }

    @Test
    @DisplayName("입찰 수는 음수만 거부하고 0은 허용한다")
    void of_입찰수_경계_검증() {
        // given-when-then
        assertThatThrownBy(() -> PriceHistory.of(closedAuction(72000L, -1), CONFIRMED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PriceHistory.of(closedAuction(72000L, 0), CONFIRMED_AT).getBidCount()).isZero();
    }

    @Test
    @DisplayName("확정시각이 null이면 예외를 던진다")
    void of_확정시각_null_예외() {
        // given-when-then
        assertThatThrownBy(() -> PriceHistory.of(closedAuction(72000L, 7), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
