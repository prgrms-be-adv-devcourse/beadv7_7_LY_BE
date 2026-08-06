package site.productservice.domain.price;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

/**
 * 확정된 거래 1건의 시세 기록. 한번 쌓인 행은 수정하지 않는다 — 과거 사실의 장부이기 때문.
 * <p>
 * 같은 거래확정 이벤트가 두 번 도착해도(재전송 등) auction_id 유니크 제약이 두 번째 저장을 거부해
 * 같은 거래가 두 줄 쌓이는 일을 막는다. 상품은 id 숫자로만 참조하고 FK 제약을 걸지 않는다—
 * 시세는 다시 만들 수 있는 파생 데이터라, 상품 병합·재적재 때 제약에 발목 잡히지 않기 위해서다.
 * <p>
 * outlier(이상치 여부)는 시세 집계에서 빼야 할 비정상 거래 표시인데, 판정 규칙은 아직 없어 항상 false로
 * 저장한다. 판정 근거가 될 입찰 수는 지금부터 쌓는다 — 나중엔 소급해서 채울 수 없기 때문.
 */
@Entity
@Table(
        name = "price_history",
        uniqueConstraints = @UniqueConstraint(name = "ukPriceHistoryAuctionId", columnNames = "auction_id"),
        indexes = @Index(name = "idxPriceHistoryMarket", columnList = "product_id, media_condition, traded_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_condition", nullable = false)
    private MediaCondition mediaCondition;

    @Column(name = "final_price", nullable = false)
    private Long finalPrice;

    /** 낙찰 시각. 시세 차트의 시간축은 이 값이다. */
    @Column(name = "traded_at", nullable = false)
    private LocalDateTime tradedAt;

    /** 거래 확정 시각 (이벤트 payload). 낙찰→확정→적재 사이 지연을 진단하는 용도라 집계엔 쓰지 않는다. */
    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @Column(name = "bid_count", nullable = false)
    private Integer bidCount;

    @Column(name = "outlier", nullable = false)
    private boolean outlier = false;

    @Column(name = "exclusion_reason")
    private String exclusionReason;

    private PriceHistory(Long auctionId, Long productId, MediaCondition mediaCondition, Long finalPrice,
            LocalDateTime tradedAt, LocalDateTime confirmedAt, Integer bidCount) {
        this.auctionId = auctionId;
        this.productId = productId;
        this.mediaCondition = mediaCondition;
        this.finalPrice = finalPrice;
        this.tradedAt = tradedAt;
        this.confirmedAt = confirmedAt;
        this.bidCount = bidCount;
    }

    public static PriceHistory of(ClosedAuction auction, LocalDateTime confirmedAt) {
        if (auction.auctionId() == null || auction.productId() == null || auction.mediaCondition() == null
                || auction.closedAt() == null) {
            throw new IllegalArgumentException("경매 정보에 빠진 필수값이 있습니다: " + auction);
        }
        if (confirmedAt == null) {
            throw new IllegalArgumentException("확정시각은 필수입니다");
        }
        if (auction.finalPrice() == null || auction.finalPrice() <= 0) {
            throw new IllegalArgumentException("낙찰가는 1 이상이어야 합니다: " + auction.finalPrice());
        }
        // 입찰 수 0은 허용 — 경매 쪽이 이 값을 어떻게 세는지 확정되지 않아, 엄격하게 막으면 정상 데이터를 잃을 수 있다
        if (auction.bidCount() == null || auction.bidCount() < 0) {
            throw new IllegalArgumentException("입찰 수는 0 이상이어야 합니다: " + auction.bidCount());
        }
        return new PriceHistory(auction.auctionId(), auction.productId(), auction.mediaCondition(),
                auction.finalPrice(), auction.closedAt(), confirmedAt, auction.bidCount());
    }
}
