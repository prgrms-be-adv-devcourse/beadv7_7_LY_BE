package site.coreservice.pointwallet.hold.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.coreservice.pointwallet.shared.Money;

@DisplayName("Hold 엔티티")
class HoldTest {

    private static final Long AUCTION_ID = 5001L;
    private static final Long USER_ID = 1L;
    private static final Money AMOUNT = Money.of(10_000);

    @Nested
    @DisplayName("생성 (place)")
    class Place {

        @Test
        @DisplayName("홀드를 생성하면 전달한 값 그대로 필드에 채워진다")
        void place_전달값_그대로_채워진다() {
            // given & when
            Hold hold = Hold.place(AUCTION_ID, USER_ID, AMOUNT);

            // then
            assertThat(hold.getAuctionId()).isEqualTo(AUCTION_ID);
            assertThat(hold.getUserId()).isEqualTo(USER_ID);
            assertThat(hold.getAmount()).isEqualTo(AMOUNT);
            assertThat(hold.getHeldAt()).isNotNull();
        }
    }
}