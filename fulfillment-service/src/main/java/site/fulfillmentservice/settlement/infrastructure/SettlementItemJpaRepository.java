package site.fulfillmentservice.settlement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.fulfillmentservice.settlement.domain.SettlementItem;
import site.fulfillmentservice.settlement.domain.SettlementStatus;

public interface SettlementItemJpaRepository extends JpaRepository<SettlementItem, Long> {

    Optional<SettlementItem> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    @Query("""
            select distinct s.sellerId from SettlementItem s
            where s.status = :status and s.completedAt < :completedAt
            """)
    List<Long> findDistinctSellerIdByStatusAndCompletedAtBefore(@Param("status") SettlementStatus status,
                                                                 @Param("completedAt") LocalDateTime completedAt);

    List<SettlementItem> findAllByStatusAndCompletedAtBeforeAndSellerId(SettlementStatus status, LocalDateTime completedAt, Long sellerId);

    @Query("""
            select s from SettlementItem s
            where s.sellerId = :sellerId
              and (:status is null or s.status = :status)
              and (:from is null or s.completedAt >= :from)
              and (:to is null or s.completedAt <= :to)
            order by s.completedAt desc, s.id desc
            """)
    Page<SettlementItem> searchBySellerId(@Param("sellerId") Long sellerId,
                                          @Param("status") SettlementStatus status,
                                          @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                          Pageable pageable);
}
