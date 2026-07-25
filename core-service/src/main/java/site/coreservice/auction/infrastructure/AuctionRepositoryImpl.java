package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AuctionRepositoryImpl implements AuctionRepository {

    private final AuctionJpaRepository jpaRepository;

    @Override
    public Auction save(Auction auction) {
        return jpaRepository.save(auction);
    }

    @Override
    public Optional<Auction> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Auction> findAllScheduledToStart(LocalDateTime threshold) {
        return jpaRepository.findAllByStatusAndStartAtLessThanEqual(AuctionStatus.SCHEDULED,
            threshold);
    }

    @Override
    public Map<Long, Long> countRunningByProductIds(List<Long> productIds) {
        return jpaRepository.countByProductIdsAndStatus(productIds, AuctionStatus.RUNNING).stream()
            .collect(Collectors.toMap(
                AuctionJpaRepository.ProductAuctionCountRow::getProductId,
                AuctionJpaRepository.ProductAuctionCountRow::getCount
            ));
    }

    @Override
    public List<Auction> findAllRunningToEnd(LocalDateTime threshold) {
        return jpaRepository.findAllByStatusAndEndAtLessThanEqual(AuctionStatus.RUNNING, threshold);
    }
}
