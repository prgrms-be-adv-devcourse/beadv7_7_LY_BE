package site.coreservice.order.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderStatus;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    boolean existsByAuctionId(Long auctionId);

    List<Order> findAllByStatusAndOrderDeadlineBefore(OrderStatus status, LocalDateTime threshold);

    List<Order> findAllByStatusAndCompletionDeadlineBefore(OrderStatus status, LocalDateTime threshold);

    @Query("""
            select o from Order o
            where o.buyerId = :buyerId
              and (:status is null or o.status = :status)
            order by o.createdAt desc, o.id desc
            """)
    Page<Order> searchByBuyerId(@Param("buyerId") Long buyerId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("""
            select o from Order o
            where o.sellerId = :sellerId
              and (:status is null or o.status = :status)
            order by o.createdAt desc, o.id desc
            """)
    Page<Order> searchBySellerId(@Param("sellerId") Long sellerId, @Param("status") OrderStatus status, Pageable pageable);
}
