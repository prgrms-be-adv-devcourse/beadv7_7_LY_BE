package site.coreservice.auction.domain;

import java.util.Optional;

public interface AuctionRepository {
    Auction save(Auction auction);

    Optional<Auction> findById(Long id);
}
