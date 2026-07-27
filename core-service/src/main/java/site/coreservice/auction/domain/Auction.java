package site.coreservice.auction.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.common.entity.BaseEntity;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Entity
@Table(name = "auction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Embedded
    private ItemInfo itemInfo;

    @Embedded
    private Pricing pricing;

    @Embedded
    private AuctionSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Embedded
    private HighestBid highestBid;

    private Auction(Long sellerId, Long productId, ItemInfo itemInfo, Pricing pricing, AuctionSchedule schedule, AuctionStatus status, HighestBid highestBid) {
        this.sellerId = sellerId;
        this.productId = productId;
        this.itemInfo = itemInfo;
        this.pricing = pricing;
        this.schedule = schedule;
        this.status = status;
        this.highestBid = highestBid;
    }

    public static Auction of(Long sellerId, Long productId, ItemInfo itemInfo, Pricing pricing, AuctionSchedule schedule, AuctionStatus status, HighestBid highestBid) {
        return new Auction(sellerId, productId, itemInfo, pricing, schedule, status, highestBid);
    }

    public static Auction register(Long sellerId, Long productId, ItemInfo itemInfo, Pricing pricing, AuctionSchedule schedule) {
        Objects.requireNonNull(sellerId, "판매자 ID는 null일 수 없습니다.");
        validateContent(productId, itemInfo, pricing, schedule);
        return new Auction(sellerId, productId, itemInfo, pricing, schedule, AuctionStatus.SCHEDULED, null);
    }

    public void modify(Long sellerId, Long productId, ItemInfo itemInfo, Pricing pricing, AuctionSchedule schedule, LocalDateTime now) {
        validateOwnership(sellerId);
        validateEditable(now);
        validateContent(productId, itemInfo, pricing, schedule);
        this.productId = productId;
        this.itemInfo = itemInfo;
        this.pricing = pricing;
        this.schedule = schedule;
    }

    public void cancel(Long sellerId, LocalDateTime now) {
        validateOwnership(sellerId);
        validateEditable(now);
        this.status = AuctionStatus.CANCELED;
    }

    private static void validateContent(Long productId, ItemInfo itemInfo, Pricing pricing, AuctionSchedule schedule) {
        Objects.requireNonNull(productId, "상품 ID는 null일 수 없습니다.");
        Objects.requireNonNull(itemInfo, "매물 정보는 null일 수 없습니다.");
        Objects.requireNonNull(pricing, "가격 정책은 null일 수 없습니다.");
        Objects.requireNonNull(schedule, "경매 일정은 null일 수 없습니다.");
    }

    private void validateEditable(LocalDateTime now) {
        if (!isEffectiveScheduledAt(now)) {
            throw new AuctionException(AuctionErrorCode.AUCTION_NOT_EDITABLE);
        }
    }

    private void validateOwnership(Long sellerId) {
        if (!this.sellerId.equals(sellerId)) {
            throw new AuctionException(AuctionErrorCode.AUCTION_ACCESS_DENIED);
        }
    }

    public void changeStatus(AuctionStatus next) {
        if (!this.status.canTransitTo(next)) {
            throw new IllegalStateException("허용되지 않은 상태 전이입니다: %s → %s".formatted(this.status, next));
        }
        this.status = next;
    }

    public boolean hasBid() {
        return highestBid != null;
    }

    public boolean isHighestBidder(Long memberId) {
        return hasBid() && highestBid.isBidder(memberId);
    }

    public AuctionStatus getEffectiveStatusAt(LocalDateTime now) {
        if(isCanceled() || isEndedWon() || isEndedFailed()) {
            return this.status;
        }
        if (!schedule.isStartedAt(now)) {
            return AuctionStatus.SCHEDULED;
        }
        if (!schedule.isEndedAt(now)) {
            return AuctionStatus.RUNNING;
        }
        return hasBid() ? AuctionStatus.ENDED_WON : AuctionStatus.ENDED_FAILED;
    }

    // status == RUNNING이어도 시작 스케줄러가 미리 바꿔둔 것일 뿐 실제로는 아직 시작 전인 상태
    public boolean isEffectiveScheduledAt(LocalDateTime now) {
        return getEffectiveStatusAt(now) == AuctionStatus.SCHEDULED;
    }

    // status == RUNNING이면서 실제로 시작 시각과 종료 시각 사이인 경우인 실제 진행 중 상태
    public boolean isEffectiveRunningAt(LocalDateTime now) {
        return status == AuctionStatus.RUNNING
                && schedule.isStartedAt(now)
                && !schedule.isEndedAt(now);
    }

    // 종료 시각은 지났지만 종료 스케줄러가 아직 status를 옮겨두지 못한 구간.
    public boolean isEffectiveClosingAt(LocalDateTime now) {
        return status == AuctionStatus.RUNNING && schedule.isEndedAt(now);
    }

    public boolean isEndedWon() {
        return status == AuctionStatus.ENDED_WON;
    }

    public boolean isEndedFailed() {
        return status == AuctionStatus.ENDED_FAILED;
    }

    public boolean isCanceled() {
        return status == AuctionStatus.CANCELED;
    }

}
