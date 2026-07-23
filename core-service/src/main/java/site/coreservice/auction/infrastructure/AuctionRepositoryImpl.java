package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;

import java.util.Optional;

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
}
