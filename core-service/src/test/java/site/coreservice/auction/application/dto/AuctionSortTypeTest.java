package site.coreservice.auction.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionSortTypeTest {

    @Test
    @DisplayName("정의된 문자열은 대응되는 AuctionSortType으로 변환된다")
    void testFrom_knownValues_returnsMatchingType() {
        assertThat(AuctionSortType.from("ending_soon")).isEqualTo(AuctionSortType.ENDING_SOON);
        assertThat(AuctionSortType.from("price_asc")).isEqualTo(AuctionSortType.PRICE_ASC);
        assertThat(AuctionSortType.from("price_desc")).isEqualTo(AuctionSortType.PRICE_DESC);
        assertThat(AuctionSortType.from("most_bids")).isEqualTo(AuctionSortType.MOST_BIDS);
    }

    @Test
    @DisplayName("null이면 기본값인 ENDING_SOON을 반환한다")
    void testFrom_null_returnsEndingSoon() {
        assertThat(AuctionSortType.from(null)).isEqualTo(AuctionSortType.ENDING_SOON);
    }

    @Test
    @DisplayName("정의되지 않은 값이면 예외를 던진다")
    void testFrom_unknownValue_throws() {
        assertThatThrownBy(() -> AuctionSortType.from("not_a_sort"))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_SORT_INVALID);
    }
}
