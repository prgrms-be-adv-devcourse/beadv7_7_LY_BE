package site.coreservice.settlement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.coreservice.settlement.domain.SettlementItem;
import site.coreservice.settlement.domain.SettlementStatus;

public interface SettlementItemJpaRepository extends JpaRepository<SettlementItem, Long> {

    Optional<SettlementItem> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<SettlementItem> findAllByStatusAndCompletedAtBefore(SettlementStatus status, LocalDateTime completedAt);

    List<SettlementItem> findAllByStatusAndCompletedAtBeforeAndSellerId(SettlementStatus status, LocalDateTime completedAt, Long sellerId);
}
