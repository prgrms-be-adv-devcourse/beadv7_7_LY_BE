package site.coreservice.settlement.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.settlement.domain.SettlementItem;

public interface SettlementItemJpaRepository extends JpaRepository<SettlementItem, Long> {

    Optional<SettlementItem> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
