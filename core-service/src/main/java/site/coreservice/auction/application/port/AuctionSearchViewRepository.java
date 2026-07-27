package site.coreservice.auction.application.port;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import site.coreservice.auction.application.dto.AuctionListQuery;
import site.coreservice.auction.application.port.dto.AuctionListSummary;
import site.coreservice.auction.application.port.dto.AuctionProductSummary;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;

import java.util.List;

public interface AuctionSearchViewRepository {
    void save(Auction auction, ProductSnapshot product, String sellerNickname);

    void updateFromAuction(Auction auction, ProductSnapshot product);

    void updateStatus(Auction auction);

    void deleteById(Long auctionId);

    List<AuctionProductSummary> findAllSummaryByIds(List<Long> auctionIds);

    Page<AuctionListSummary> search(AuctionListQuery query, Pageable pageable);

}
