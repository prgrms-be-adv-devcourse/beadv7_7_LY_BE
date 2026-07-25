package site.coreservice.order.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderStatus;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    boolean existsByAuctionId(Long auctionId);

    List<Order> findAllByStatusAndOrderDeadlineBefore(OrderStatus status, LocalDateTime threshold);
}
