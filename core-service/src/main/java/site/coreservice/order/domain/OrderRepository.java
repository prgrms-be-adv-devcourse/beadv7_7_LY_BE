package site.coreservice.order.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    boolean existsByAuctionId(Long auctionId);

    List<Order> findAllByStatusAndOrderDeadlineBefore(OrderStatus status, LocalDateTime threshold);

    List<Order> findAllByStatusAndCompletionDeadlineBefore(OrderStatus status, LocalDateTime threshold);
}
