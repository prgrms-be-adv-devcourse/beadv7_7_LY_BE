package site.auctionservice.application.port;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import site.auctionservice.application.dto.AuctionListQuery;
import site.auctionservice.application.port.dto.AuctionListSummary;
import site.auctionservice.application.port.dto.AuctionProductSummary;
import site.auctionservice.application.port.dto.ProductSnapshot;
import site.auctionservice.domain.Auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AuctionSearchViewRepository {

    void save(Auction auction, ProductSnapshot product, String sellerNickname);

    void updateFromAuction(Auction auction, ProductSnapshot product);

    void deleteById(Long auctionId);

    List<AuctionProductSummary> findAllSummaryByIds(List<Long> auctionIds);

    Page<AuctionListSummary> search(AuctionListQuery query, Pageable pageable);

    void updateOnBid(Long auctionId, BigDecimal highestBidAmount, int bidCount,
        LocalDateTime endAt);

}
