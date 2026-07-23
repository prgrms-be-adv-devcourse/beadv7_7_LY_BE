package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionTest {

    private final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", null);
    private final Pricing pricing = Pricing.of(Money.from(1_000L), Money.from(10L), Money.from(0L));
    private final AuctionSchedule schedule = AuctionSchedule.of(
            Period.of(LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0)),
            false, null
    );

    @Test
    @DisplayName("register()로 생성하면 SCHEDULED 상태이고 입찰이 없다")
    void testRegister_createsScheduledAuctionWithoutBid() {
        // when
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
        assertThat(auction.hasBid()).isFalse();
    }

    @Test
    @DisplayName("필수 값이 null이면 register()는 예외를 던진다")
    void testRegister_nullRequiredFields_throws() {
        // when & then
        assertThatThrownBy(() -> Auction.register(null, 100L, itemInfo, pricing, schedule)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Auction.register(1L, null, itemInfo, pricing, schedule)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Auction.register(1L, 100L, null, pricing, schedule)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Auction.register(1L, 100L, itemInfo, null, schedule)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Auction.register(1L, 100L, itemInfo, pricing, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SCHEDULED에서 RUNNING으로 상태를 전이할 수 있다")
    void testChangeStatus_scheduledToRunning_succeeds() {
        // given
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);

        // when
        auction.changeStatus(AuctionStatus.RUNNING);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    @DisplayName("허용되지 않은 상태 전이를 시도하면 예외가 발생한다")
    void testChangeStatus_invalidTransition_throws() {
        // given
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);

        // when & then
        assertThatThrownBy(() -> auction.changeStatus(AuctionStatus.ENDED_WON)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("종료 상태에서는 더 이상 전이할 수 없다")
    void testChangeStatus_ofTerminalStatus_throws() {
        // given
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);
        auction.changeStatus(AuctionStatus.CANCELED);

        // when & then
        assertThatThrownBy(() -> auction.changeStatus(AuctionStatus.RUNNING)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("최고입찰 정보가 있으면 hasBid()는 true를 반환한다")
    void testHasBid_trueWhenHighestBidPresent() {
        // given
        HighestBid highestBid = HighestBid.of(Money.from(1_500L), 2L, 10L);

        // when
        Auction auction = Auction.of(1L, 100L, itemInfo, pricing, schedule, AuctionStatus.RUNNING, highestBid);

        // then
        assertThat(auction.hasBid()).isTrue();
    }
}
