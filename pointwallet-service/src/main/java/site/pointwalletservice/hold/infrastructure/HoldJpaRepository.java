package site.pointwalletservice.hold.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.pointwalletservice.hold.domain.Hold;

public interface HoldJpaRepository extends JpaRepository<Hold, Long> {
    Optional<Hold> findByAuctionId(Long auctionId);
}