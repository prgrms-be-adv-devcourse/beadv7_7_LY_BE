package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import site.coreservice.auction.domain.Bid;
import site.coreservice.auction.domain.BidOutcome;
import site.coreservice.auction.domain.BidRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public Page<Bid> findLatestBidsByBidder(Long bidderId, Pageable pageable) {
        List<Bid> content = jpaRepository.findLatestBidsByBidder(bidderId, pageable);
        long total = jpaRepository.countDistinctAuctionsByBidder(bidderId);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Map<Long, Long> countGroupedByAuctionIds(List<Long> auctionIds) {
        if (auctionIds.isEmpty()) return Map.of();
        return jpaRepository.countGroupedByAuctionIds(auctionIds).stream()
                .collect(Collectors.toMap(BidJpaRepository.AuctionBidCount::getAuctionId,
                        BidJpaRepository.AuctionBidCount::getCnt));
    }
}
