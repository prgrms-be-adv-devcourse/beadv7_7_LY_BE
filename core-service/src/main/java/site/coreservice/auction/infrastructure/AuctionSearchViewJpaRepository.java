package site.coreservice.auction.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionSearchViewJpaRepository extends JpaRepository<AuctionSearchView, Long> {
}
