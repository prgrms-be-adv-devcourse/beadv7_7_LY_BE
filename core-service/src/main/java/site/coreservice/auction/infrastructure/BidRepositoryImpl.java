package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.auction.domain.Bid;
import site.coreservice.auction.domain.BidOutcome;
import site.coreservice.auction.domain.BidRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BidRepositoryImpl implements BidRepository {
    private final BidJpaRepository jpaRepository;

    @Override
    public Bid save(Bid bid) {
        return jpaRepository.save(bid);
    }

    @Override
    public Optional<Bid> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Bid> findActiveBid(Long auctionId) {
        return jpaRepository.findByAuctionIdAndOutcome(auctionId, BidOutcome.ACTIVE);
    }
}
