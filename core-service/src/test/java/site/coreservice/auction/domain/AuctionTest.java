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
    private final LocalDateTime registerNow = schedule.getPeriod().getStartAt().minusHours(1);
    // 수정/취소 마감 시한(10분) 밖: 성공 케이스 검증용
    private final LocalDateTime wellBeforeStart = schedule.getPeriod().getStartAt().minusMinutes(15);
    // 수정/취소 마감 시한(10분) 안: 실패 케이스 검증용
    private final LocalDateTime withinEditDeadline = schedule.getPeriod().getStartAt().minusMinutes(5);

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
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule, registerNow);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
        assertThat(auction.hasBid()).isFalse();
    }

    @Test
    @DisplayName("시작 시각이 현재로부터 30분 이내면 register()는 예외를 던진다")
    void testRegister_startTimeTooSoon_throws() {
        // given: 시작 시각이 now로부터 정확히 30분 미만
        LocalDateTime tooSoonNow = schedule.getPeriod().getStartAt().minusMinutes(29);

        // when & then
        assertThatThrownBy(() -> Auction.register(1L, 100L, itemInfo, pricing, schedule, tooSoonNow))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_START_TOO_SOON);
    }

    @Test
    @DisplayName("필수 값이 null이면 register()는 예외를 던진다")
    void testRegister_nullRequiredFields_throws() {
        // when & then
        assertThatThrownBy(() -> Auction.register(null, 100L, itemInfo, pricing, schedule, registerNow)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Auction.register(1L, null, itemInfo, pricing, schedule, registerNow)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Auction.register(1L, 100L, null, pricing, schedule, registerNow)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Auction.register(1L, 100L, itemInfo, null, schedule, registerNow)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Auction.register(1L, 100L, itemInfo, pricing, null, registerNow)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SCHEDULED에서 RUNNING으로 상태를 전이할 수 있다")
    void testChangeStatus_scheduledToRunning_succeeds() {
        // given
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule, registerNow);

        // when
        auction.changeStatus(AuctionStatus.RUNNING);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    @DisplayName("허용되지 않은 상태 전이를 시도하면 예외가 발생한다")
    void testChangeStatus_invalidTransition_throws() {
        // given
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule, registerNow);

        // when & then
        assertThatThrownBy(() -> auction.changeStatus(AuctionStatus.ENDED_WON)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("종료 상태에서는 더 이상 전이할 수 없다")
    void testChangeStatus_ofTerminalStatus_throws() {
        // given
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule, registerNow);
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
        LocalDateTime newStartAt = wellBeforeStart.plusMinutes(31);
        AuctionSchedule updatedSchedule = AuctionSchedule.of(
                Period.of(newStartAt, newStartAt.plusHours(2)), false, null);

        // when
        auction.modify(1L, 200L, itemInfo, pricing, updatedSchedule, wellBeforeStart);

        // then
        assertThat(auction.getProductId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("수정 시 다시 설정하는 startAt이 현재로부터 30분 이내면, 기존 startAt이 마감 시한 전이어도 예외를 던진다")
    void testModify_newStartAtTooSoon_throws() {
        // given: 기존 startAt까지는 아직 15분 남아 편집 마감(10분)엔 안 걸리지만,
        // 새로 설정하려는 startAt이 now로부터 10분 후라 리드타임(30분) 미만이다
        Auction auction = auctionWith(AuctionStatus.SCHEDULED);
        LocalDateTime tooSoonNewStartAt = wellBeforeStart.plusMinutes(10);
        AuctionSchedule tooSoonSchedule = AuctionSchedule.of(
                Period.of(tooSoonNewStartAt, tooSoonNewStartAt.plusHours(2)), false, null);

        // when & then
        assertThatThrownBy(() -> auction.modify(1L, 200L, itemInfo, pricing, tooSoonSchedule, wellBeforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_START_TOO_SOON);
    }

    @Test
    @DisplayName("시작 시각 10분 이내로 임박하면 아직 SCHEDULED여도 수정할 수 없다")
    void testModify_withinEditDeadline_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED);

        // when & then
        assertThatThrownBy(() -> auction.modify(1L, 200L, itemInfo, pricing, schedule, withinEditDeadline))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
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
        auction.cancel(1L, wellBeforeStart);

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
        auction.cancel(1L, wellBeforeStart);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
    }

    @Test
    @DisplayName("시작 시각 10분 이내로 임박하면 아직 SCHEDULED여도 취소할 수 없다")
    void testCancel_withinEditDeadline_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED);

        // when & then
        assertThatThrownBy(() -> auction.cancel(1L, withinEditDeadline))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
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

    @Test
    @DisplayName("입찰 조건을 만족하면 applyBid()로 최고입찰이 갱신된다")
    void testApplyBid_validBid_updatesHighestBid() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when
        auction.applyBid(2L, Money.of(1_100L), 10L, afterStart);

        // then
        assertThat(auction.getHighestBid().getAmount()).isEqualTo(Money.of(1_100L));
        assertThat(auction.getHighestBid().isBidder(2L)).isTrue();
        assertThat(auction.getHighestBid().getBidId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("취소된 경매는 입찰할 수 없다")
    void testValidateBiddable_notRunning_throws() {
        // given: CANCELED는 시각과 무관하게 실효 상태도 항상 CANCELED다
        Auction auction = auctionWith(AuctionStatus.CANCELED);

        // when & then
        assertThatThrownBy(() -> auction.validateBiddable(2L, Money.of(1_100L), afterStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_BIDDABLE);
    }

    @Test
    @DisplayName("시작 시각 전이면 입찰할 수 없다")
    void testValidateBiddable_beforeStart_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when & then
        assertThatThrownBy(() -> auction.validateBiddable(2L, Money.of(1_100L), beforeStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_BIDDABLE);
    }

    @Test
    @DisplayName("종료 시각이 지났으면 입찰할 수 없다")
    void testValidateBiddable_afterEnd_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when & then
        assertThatThrownBy(() -> auction.validateBiddable(2L, Money.of(1_100L), afterEnd))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_BIDDABLE);
    }

    @Test
    @DisplayName("판매자 본인은 자기 경매에 입찰할 수 없다")
    void testValidateBiddable_seller_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when & then
        assertThatThrownBy(() -> auction.validateBiddable(1L, Money.of(1_100L), afterStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_SELLER_CANNOT_BID);
    }

    @Test
    @DisplayName("현재 최고입찰자는 다시 입찰할 수 없다")
    void testValidateBiddable_currentHighestBidder_throws() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(1_100L), 2L, 10L);
        Auction auction = auctionWith(AuctionStatus.RUNNING, highestBid);

        // when & then
        assertThatThrownBy(() -> auction.validateBiddable(2L, Money.of(1_200L), afterStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.ALREADY_HIGHEST_BIDDER);
    }

    @Test
    @DisplayName("최소 입찰가 미만이면 입찰할 수 없다")
    void testValidateBiddable_belowMinimum_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING);

        // when & then: pricing = 시작가 1_000 + 배송비 0, 최소 입찰가는 1_000
        assertThatThrownBy(() -> auction.validateBiddable(2L, Money.of(999L), afterStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_AMOUNT_TOO_LOW);
    }

    @Test
    @DisplayName("기존 최고입찰가 + 입찰단위 미만이면 입찰할 수 없다")
    void testValidateBiddable_belowNextMinBidAmount_throws() {
        // given: bidUnit = 100, 기존 최고입찰 1_100 → 다음 최소 입찰가는 1_200
        HighestBid highestBid = HighestBid.of(Money.of(1_100L), 2L, 10L);
        Auction auction = auctionWith(AuctionStatus.RUNNING, highestBid);

        // when & then
        assertThatThrownBy(() -> auction.validateBiddable(3L, Money.of(1_150L), afterStart))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_AMOUNT_TOO_LOW);
    }
}
