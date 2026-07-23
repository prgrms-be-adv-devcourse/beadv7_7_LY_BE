package site.coreservice.auction.infrastructure;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 목록 조회를 위한 read model(비정규화된 조회 전용 뷰).
 * 경매 목록에는 경매+상품 정보를 함께 표시해야 하고 필터링 조건도 경매/상품 필드가 혼재되어 있어,
 * 경매 생성 시점에 경매·상품·판매자 정보를 합쳐 이 테이블에 저장해두고,
 * 목록 조회 시에는 다른 곳을 조회하지 않고 이 테이블만 읽도록 하기 위해 도입함.
 * 도메인 규칙이나 불변식을 갖지 않으므로 domain 계층이 아닌 infra 계층에 둠.
 */
@Entity
@Table(name = "auction_search_view")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionSearchView {
    @Id
    @Column(name = "auction_id")
    private Long auctionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String title;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column
    private String genre;

    @Column(name = "press_type", nullable = false)
    private String pressType;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private ItemCondition itemCondition;

    @Column
    private String thumbnail;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_nickname", nullable = false)
    private String sellerNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    @Column(name = "highest_bid_amount")
    private BigDecimal highestBidAmount;

    @Column(name = "bid_count", nullable = false)
    private Integer bidCount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    private AuctionSearchView(Long id, Long productId, String title, String artistName, Integer releaseYear, String genre, String pressType, ItemCondition itemCondition, String thumbnail, Long sellerId, String sellerNickname, AuctionStatus status, BigDecimal highestBidAmount, Integer bidCount, LocalDateTime startAt, LocalDateTime endAt) {
        this.auctionId = id;
        this.productId = productId;
        this.title = title;
        this.artistName = artistName;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.pressType = pressType;
        this.itemCondition = itemCondition;
        this.thumbnail = thumbnail;
        this.sellerId = sellerId;
        this.sellerNickname = sellerNickname;
        this.status = status;
        this.highestBidAmount = highestBidAmount;
        this.bidCount = bidCount;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static AuctionSearchView from(Auction auction, ProductSnapshot productSnapshot, String sellerNickname) {
        Money currentPrice = auction.getPricing().getStartPrice().plus(auction.getPricing().getShippingFee());
        List<String> imageUrls = auction.getItemInfo().getImageUrls();
        String thumbnail = (imageUrls == null || imageUrls.isEmpty()) ? null : imageUrls.getFirst();
        return new AuctionSearchView(
                auction.getId(),
                auction.getProductId(),
                productSnapshot.title(),
                productSnapshot.artistName(),
                productSnapshot.releaseYear(),
                productSnapshot.genre(),
                productSnapshot.pressType(),
                auction.getItemInfo().getCondition(),
                thumbnail,
                auction.getSellerId(),
                sellerNickname,
                auction.getStatus(),
                currentPrice.getValue(),
                0,
                auction.getSchedule().getPeriod().getStartAt(),
                auction.getSchedule().getPeriod().getEndAt()
        );
    }
}
