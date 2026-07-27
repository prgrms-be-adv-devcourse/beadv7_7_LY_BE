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

    /** status는 nullable — null이면 해당 조건 미적용. 정렬은 createdAt desc, id desc 고정. */
    OrderSearchPage findAllByBuyerId(Long buyerId, OrderStatus status, int page, int size);

    /** status는 nullable — null이면 해당 조건 미적용. 정렬은 createdAt desc, id desc 고정. */
    OrderSearchPage findAllBySellerId(Long sellerId, OrderStatus status, int page, int size);
}
