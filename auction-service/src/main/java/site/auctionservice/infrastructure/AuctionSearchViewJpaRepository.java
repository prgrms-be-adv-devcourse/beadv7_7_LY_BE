package site.auctionservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuctionSearchViewJpaRepository
        extends JpaRepository<AuctionSearchView, Long>, JpaSpecificationExecutor<AuctionSearchView> {
}
