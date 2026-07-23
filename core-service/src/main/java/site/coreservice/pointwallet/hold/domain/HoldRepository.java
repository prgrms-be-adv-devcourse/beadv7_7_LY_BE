package site.coreservice.pointwallet.hold.domain;

import java.util.Optional;

public interface HoldRepository {

    Hold save(Hold hold);

    Optional<Hold> findByAuctionId(Long auctionId);

    void delete(Hold hold);
}