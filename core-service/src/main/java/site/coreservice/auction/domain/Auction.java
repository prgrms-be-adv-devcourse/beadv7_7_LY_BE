package site.coreservice.auction.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.common.entity.BaseEntity;

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

    public static Auction from(Long sellerId, Long productId, ItemInfo itemInfo, Pricing pricing, AuctionSchedule schedule, AuctionStatus status, HighestBid highestBid) {
        return new Auction(sellerId, productId, itemInfo, pricing, schedule, status, highestBid);
    }

    public static Auction register(Long sellerId, Long productId, ItemInfo itemInfo, Pricing pricing, AuctionSchedule schedule) {
        validate(sellerId, productId, itemInfo, pricing, schedule);
        return new Auction(sellerId, productId, itemInfo, pricing, schedule, AuctionStatus.SCHEDULED, null);
    }

    private static void validate(Long sellerId, Long productId, ItemInfo itemInfo, Pricing pricing, AuctionSchedule schedule) {
        Objects.requireNonNull(sellerId, "판매자 ID는 null일 수 없습니다.");
        Objects.requireNonNull(productId, "상품 ID는 null일 수 없습니다.");
        Objects.requireNonNull(itemInfo, "매물 정보는 null일 수 없습니다.");
        Objects.requireNonNull(pricing, "가격 정책은 null일 수 없습니다.");
        Objects.requireNonNull(schedule, "경매 일정은 null일 수 없습니다.");
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

}
