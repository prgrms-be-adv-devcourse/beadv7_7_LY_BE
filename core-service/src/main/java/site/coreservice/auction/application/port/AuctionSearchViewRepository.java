package site.coreservice.auction.application.port;

import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;

public interface AuctionSearchViewRepository {
    void save(Auction auction, ProductSnapshot product, String sellerNickname);
}
