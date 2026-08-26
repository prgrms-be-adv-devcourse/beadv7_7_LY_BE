package site.pointwalletservice.hold.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.pointwalletservice.shared.Money;

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

    @Nested
    @DisplayName("동일 요청 판단 (isSameRequest)")
    class IsSameRequest {

        @Test
        @DisplayName("userId와 amount가 모두 같으면 true - 실질적으로 같은 홀드에 대한 재요청이다")
        void userId와_amount가_모두_같으면_true() {
            // given
            Hold hold = Hold.place(AUCTION_ID, USER_ID, AMOUNT);

            // when & then
            assertThat(hold.isSameRequest(USER_ID, AMOUNT)).isTrue();
        }

        @Test
        @DisplayName("userId가 다르면 false - 최고 입찰자가 바뀌는 실질적 변화다")
        void userId가_다르면_false() {
            // given
            Hold hold = Hold.place(AUCTION_ID, USER_ID, AMOUNT);
            Long otherUserId = USER_ID + 1;

            // when & then
            assertThat(hold.isSameRequest(otherUserId, AMOUNT)).isFalse();
        }

        @Test
        @DisplayName("amount가 다르면 false - 같은 유저라도 입찰가가 바뀌는 실질적 변화다")
        void amount가_다르면_false() {
            // given
            Hold hold = Hold.place(AUCTION_ID, USER_ID, AMOUNT);
            Money differentAmount = AMOUNT.add(Money.of(1_000));

            // when & then
            assertThat(hold.isSameRequest(USER_ID, differentAmount)).isFalse();
        }
    }
}