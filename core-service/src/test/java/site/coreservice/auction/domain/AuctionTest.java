package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionTest {

    private final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", null);
    private final Pricing pricing = Pricing.of(Money.of(1_000L), Money.of(100L), Money.of(0L));
    private final AuctionSchedule schedule = AuctionSchedule.of(
            Period.of(LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0)),
            false, null
    );
    private final LocalDateTime beforeStart = schedule.getPeriod().getStartAt().minusMinutes(1);
    private final LocalDateTime afterStart = schedule.getPeriod().getStartAt().plusMinutes(1);
    private final LocalDateTime afterEnd = schedule.getPeriod().getEndAt().plusMinutes(1);

    private Auction auctionWith(AuctionStatus status) {
        return auctionWith(status, null);
    }

    private Auction auctionWith(AuctionStatus status, HighestBid highestBid) {
        return Auction.of(1L, 100L, itemInfo, pricing, schedule, status, highestBid);
    }

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
        HighestBid highestBid = HighestBid.of(Money.of(1_500L), 2L, 10L);

        // when
        Auction auction = Auction.of(1L, 100L, itemInfo, pricing, schedule, AuctionStatus.RUNNING, highestBid);

        // then
        assertThat(auction.hasBid()).isTrue();
    }

    @Test
    @DisplayName("판매자 본인이 아니면 수정할 수 없다")
    void testModify_notOwner_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED);

        // when & then
        assertThatThrownBy(() -> auction.modify(2L, 200L, itemInfo, pricing, schedule, beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("SCHEDULED 상태여도 시작 시각이 지났으면 실제로는 RUNNING이라 수정할 수 없다")
    void testModify_scheduledStatus_afterStartTime_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED);

        // when & then
        assertThatThrownBy(() -> auction.modify(1L, 200L, itemInfo, pricing, schedule, afterStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
    }

    @Test
    @DisplayName("RUNNING 상태이고 시작 시각 이전이면 수정할 수 있다")
    void testModify_runningStatus_beforeStartTime_succeeds() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when
        auction.modify(1L, 200L, itemInfo, pricing, schedule, beforeStart);

        // then
        assertThat(auction.getProductId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("RUNNING 상태이고 시작 시각이 지났으면 수정할 수 없다")
    void testModify_runningStatus_afterStartTime_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when & then
        assertThatThrownBy(() -> auction.modify(1L, 200L, itemInfo, pricing, schedule, afterStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
    }

    @Test
    @DisplayName("CANCELED 상태면 수정할 수 없다")
    void testModify_canceledStatus_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.CANCELED);

        // when & then
        assertThatThrownBy(() -> auction.modify(1L, 200L, itemInfo, pricing, schedule, beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
    }

    @Test
    @DisplayName("종료된 상태(ENDED_WON, ENDED_FAILED)면 수정할 수 없다")
    void testModify_endedStatus_throws() {
        // given
        Auction wonAuction = auctionWith(AuctionStatus.ENDED_WON);
        Auction failedAuction = auctionWith(AuctionStatus.ENDED_FAILED);

        // when & then
        assertThatThrownBy(() -> wonAuction.modify(1L, 200L, itemInfo, pricing, schedule, beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
        assertThatThrownBy(() -> failedAuction.modify(1L, 200L, itemInfo, pricing, schedule, beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
    }

    @Test
    @DisplayName("판매자 본인이 SCHEDULED 상태의 경매를 취소하면 CANCELED로 전이된다")
    void testCancel_ownerOnScheduledAuction_succeeds() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED);

        // when
        auction.cancel(1L, beforeStart);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
    }

    @Test
    @DisplayName("판매자 본인이 아니면 취소할 수 없다")
    void testCancel_notOwner_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED);

        // when & then
        assertThatThrownBy(() -> auction.cancel(2L, beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("RUNNING 상태이고 시작 시각 이전이면 취소할 수 있다")
    void testCancel_runningStatus_beforeStartTime_succeeds() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when
        auction.cancel(1L, beforeStart);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
    }

    @Test
    @DisplayName("RUNNING 상태이고 시작 시각이 지났으면 취소할 수 없다")
    void testCancel_runningStatus_afterStartTime_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when & then
        assertThatThrownBy(() -> auction.cancel(1L, afterStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
    }

    @Test
    @DisplayName("CANCELED 상태면 다시 취소할 수 없다")
    void testCancel_canceledStatus_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.CANCELED);

        // when & then
        assertThatThrownBy(() -> auction.cancel(1L, beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
    }

    @Test
    @DisplayName("종료된 상태(ENDED_WON, ENDED_FAILED)면 취소할 수 없다")
    void testCancel_endedStatus_throws() {
        // given
        Auction wonAuction = auctionWith(AuctionStatus.ENDED_WON);
        Auction failedAuction = auctionWith(AuctionStatus.ENDED_FAILED);

        // when & then
        assertThatThrownBy(() -> wonAuction.cancel(1L, beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
        assertThatThrownBy(() -> failedAuction.cancel(1L, beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
    }

    @Test
    @DisplayName("CANCELED, ENDED_WON, ENDED_FAILED은 시각과 무관하게 실제 상태도 그대로 유지된다")
    void testGetEffectiveStatusAt_terminalStatuses_ignoreTime() {
        assertThat(auctionWith(AuctionStatus.CANCELED).getEffectiveStatusAt(beforeStart)).isEqualTo(AuctionStatus.CANCELED);
        assertThat(auctionWith(AuctionStatus.ENDED_WON).getEffectiveStatusAt(beforeStart)).isEqualTo(AuctionStatus.ENDED_WON);
        assertThat(auctionWith(AuctionStatus.ENDED_FAILED).getEffectiveStatusAt(afterEnd)).isEqualTo(AuctionStatus.ENDED_FAILED);
    }

    @Test
    @DisplayName("RUNNING이어도 시작 시각 전이면 실제 상태는 SCHEDULED다 (시작 스케줄러가 미리 바꿔둔 경우)")
    void testGetEffectiveStatusAt_runningStatus_beforeStart_isScheduled() {
        assertThat(auctionWith(AuctionStatus.RUNNING).getEffectiveStatusAt(beforeStart)).isEqualTo(AuctionStatus.SCHEDULED);
    }

    @Test
    @DisplayName("RUNNING이고 시작~종료 사이면 실제 상태도 RUNNING이다")
    void testGetEffectiveStatusAt_runningStatus_inProgress_isRunning() {
        assertThat(auctionWith(AuctionStatus.RUNNING).getEffectiveStatusAt(afterStart)).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    @DisplayName("RUNNING이고 종료 시각이 지났으면 입찰 여부로 ENDED_WON/ENDED_FAILED를 판정한다")
    void testGetEffectiveStatusAt_runningStatus_afterEnd_resolvesByBid() {
        HighestBid highestBid = HighestBid.of(Money.of(1_500L), 2L, 10L);

        assertThat(auctionWith(AuctionStatus.RUNNING, highestBid).getEffectiveStatusAt(afterEnd)).isEqualTo(AuctionStatus.ENDED_WON);
        assertThat(auctionWith(AuctionStatus.RUNNING).getEffectiveStatusAt(afterEnd)).isEqualTo(AuctionStatus.ENDED_FAILED);
    }

    @Test
    @DisplayName("isEffectiveScheduledAt은 SCHEDULED이거나 RUNNING+시작 전일 때만 true다")
    void testIsEffectiveScheduledAt() {
        assertThat(auctionWith(AuctionStatus.SCHEDULED).isEffectiveScheduledAt(beforeStart)).isTrue();
        assertThat(auctionWith(AuctionStatus.RUNNING).isEffectiveScheduledAt(beforeStart)).isTrue();
        assertThat(auctionWith(AuctionStatus.RUNNING).isEffectiveScheduledAt(afterStart)).isFalse();
        assertThat(auctionWith(AuctionStatus.ENDED_WON).isEffectiveScheduledAt(beforeStart)).isFalse();
    }

    @Test
    @DisplayName("isEffectiveRunningAt은 RUNNING이면서 실제 시작~종료 사이일 때만 true다")
    void testIsEffectiveRunningAt() {
        assertThat(auctionWith(AuctionStatus.RUNNING).isEffectiveRunningAt(beforeStart)).isFalse();
        assertThat(auctionWith(AuctionStatus.RUNNING).isEffectiveRunningAt(afterStart)).isTrue();
        assertThat(auctionWith(AuctionStatus.RUNNING).isEffectiveRunningAt(afterEnd)).isFalse();
        assertThat(auctionWith(AuctionStatus.SCHEDULED).isEffectiveRunningAt(afterStart)).isFalse();
    }

    @Test
    @DisplayName("isEffectiveClosingAt은 RUNNING이면서 종료 시각이 지났을 때만 true다")
    void testIsEffectiveClosingAt() {
        assertThat(auctionWith(AuctionStatus.RUNNING).isEffectiveClosingAt(afterEnd)).isTrue();
        assertThat(auctionWith(AuctionStatus.RUNNING).isEffectiveClosingAt(afterStart)).isFalse();
        assertThat(auctionWith(AuctionStatus.ENDED_WON).isEffectiveClosingAt(afterEnd)).isFalse();
    }

    @Test
    @DisplayName("[알려진 한계] status가 아직 SCHEDULED인 채로 종료 시각까지 지나면 isEffectiveClosingAt은 이를 감지하지 못한다")
    void testIsEffectiveClosingAt_knownLimitation_whenStatusStillScheduled() {
        // 시작 스케줄러조차 돌지 못한 극단적 지연 상황을 가정한다.
        // getEffectiveStatusAt은 시간 기반으로 ENDED_FAILED를 정확히 계산하지만,
        // isEffectiveClosingAt은 status == RUNNING만 확인하므로 이 케이스는 CLOSING으로 잡아내지 못한다.
        Auction auction = auctionWith(AuctionStatus.SCHEDULED);

        assertThat(auction.getEffectiveStatusAt(afterEnd)).isEqualTo(AuctionStatus.ENDED_FAILED);
        assertThat(auction.isEffectiveClosingAt(afterEnd)).isFalse();
    }
}
