package site.coreservice.auction.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.auction.domain.Auction;

public interface AuctionJpaRepository extends JpaRepository<Auction, Long> {
}
