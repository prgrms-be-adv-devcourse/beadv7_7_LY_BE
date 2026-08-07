package site.auctionservice.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.NonNull;
import site.auctionservice.application.dto.AuctionDetailResult;
import site.auctionservice.application.dto.AuctionStatusDetail;
import site.auctionservice.application.dto.BidDetailResult;
import site.auctionservice.application.port.dto.ProductDetail;
import site.auctionservice.domain.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuctionDetailResponse(
        // 공통 (항상 채워짐)
        @NonNull Long auctionId,
        @NonNull String status,
        @NonNull String itemCondition,
        String itemDescription,
        List<String> itemImages,
        @NonNull BigDecimal startBidAmount,
        @NonNull BigDecimal bidUnit,
        @NonNull LocalDateTime startAt,
        @NonNull LocalDateTime endAt,
        @NonNull Boolean extensionEnabled,
        Integer extensionTime,
        @NonNull ProductInfo product,
        @NonNull SellerInfo seller,

        // RUNNING 전용
        BigDecimal highestBidAmount,
        BigDecimal nextMinBidAmount,
        Long bidCount,
        List<BidInfo> recentBids,
        Boolean myHighest,

        // ENDED_WON/ENDED_FAILED 공통
        Boolean won,

        // ENDED_WON 전용
        BidInfo winningBid
) {
    private static final String UNKNOWN_RELEASE_YEAR = "발매연도 미상";
    private static final String CLOSING_STATUS = "CLOSING";

    public static AuctionDetailResponse from(AuctionDetailResult result) {
        AuctionDetailResponseBuilder builder = AuctionDetailResponse.builder()
                .auctionId(result.auctionId())
                .itemCondition(result.itemCondition())
                .itemDescription(result.itemDescription())
                .itemImages(result.itemImages())
                .startBidAmount(result.startBidAmount())
                .bidUnit(result.bidUnit())
                .startAt(result.startAt())
                .endAt(result.endAt())
                .extensionEnabled(result.extensionEnabled())
                .extensionTime(result.extensionTime())
                .product(ProductInfo.from(result.product()))
                .seller(SellerInfo.of(result.sellerId(), result.sellerNickname()));

        switch (result.detail()) {
            case AuctionStatusDetail.ScheduledDetail scheduled -> builder.status(AuctionStatus.SCHEDULED.name());
            case AuctionStatusDetail.RunningDetail running -> builder.status(AuctionStatus.RUNNING.name())
                    .highestBidAmount(running.highestBidAmount())
                    .nextMinBidAmount(running.nextMinBidAmount())
                    .bidCount(running.bidCount())
                    .recentBids(running.bidDetails().stream().map(BidInfo::from).toList())
                    .myHighest(running.myHighest());
            case AuctionStatusDetail.ClosingDetail closing -> builder.status(CLOSING_STATUS);
            case AuctionStatusDetail.EndedWonDetail endedWon -> builder.status(AuctionStatus.ENDED_WON.name())
                    .won(true)
                    .winningBid(BidInfo.from(endedWon.bidDetail()));
            case AuctionStatusDetail.EndedFailedDetail endedFailed -> builder.status(AuctionStatus.ENDED_FAILED.name())
                    .won(false);
        }

        return builder.build();
    }

    public record ProductInfo(Long productId, String title, String artistName, String coverImageUrl, String releaseYear,
                       String pressType, String genre) {
        static ProductInfo from(ProductDetail d) {
            String releaseYear = d.releaseYear() == null ? UNKNOWN_RELEASE_YEAR : String.valueOf(d.releaseYear());
            return new ProductInfo(d.productId(), d.title(), d.artistName(), d.coverImageUrl(),
                    releaseYear, d.pressType(), d.genre());
        }
    }

    public record SellerInfo(Long sellerId, String nickname) {
        static SellerInfo of(Long sellerId, String nickname) {
            return new SellerInfo(sellerId, nickname);
        }
    }

    public record BidInfo(String bidder, BigDecimal amount, LocalDateTime placedAt) {
        static BidInfo from(BidDetailResult bid) {
            return new BidInfo(bid.bidderNickname(), bid.amount(), bid.placedAt());
        }
    }
}
