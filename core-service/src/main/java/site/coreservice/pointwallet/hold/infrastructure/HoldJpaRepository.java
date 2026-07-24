package site.coreservice.pointwallet.hold.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.pointwallet.hold.domain.Hold;

public interface HoldJpaRepository extends JpaRepository<Hold, Long> {
    Optional<Hold> findByAuctionId(Long auctionId);
}