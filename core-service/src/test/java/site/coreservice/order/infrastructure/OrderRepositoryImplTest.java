package site.coreservice.order.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.coreservice.order.domain.DeliveryInfo;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderItemSnapshot;
import site.coreservice.order.domain.OrderRepository;
import site.coreservice.order.domain.OrderStatus;
import site.coreservice.support.RepositoryTest;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@Import(OrderRepositoryImpl.class)
class OrderRepositoryImplTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order pendingOrder(Long auctionId, LocalDateTime orderDeadline) {
        OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
        return Order.of(auctionId, 1201L, 301L, 302L, BigDecimal.valueOf(85_000), orderDeadline, itemSnapshot);
    }

    @Test
    @DisplayName("기한이 지난 PENDING 주문만 조회된다")
    void findAllByStatusAndOrderDeadlineBefore_returnsOnlyExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        Order expiredPending = orderRepository.save(pendingOrder(5001L, now.minusHours(1)));
        orderRepository.save(pendingOrder(5002L, now.plusHours(1)));

        LocalDateTime orderedDeadline = now.minusHours(1);
        Order alreadyOrdered = pendingOrder(5003L, orderedDeadline);
        LocalDateTime confirmedAt = orderedDeadline.minusMinutes(30);
        alreadyOrdered.confirmOrder(
                DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호"),
                confirmedAt.plusDays(7), confirmedAt);
        orderRepository.save(alreadyOrdered);

        List<Order> result = orderRepository.findAllByStatusAndOrderDeadlineBefore(OrderStatus.PENDING, now);

        assertThat(result).extracting(Order::getId).containsExactly(expiredPending.getId());
    }

    @Test
    @DisplayName("대상이 없으면 빈 목록을 반환한다")
    void findAllByStatusAndOrderDeadlineBefore_returnsEmptyWhenNoneExpired() {
        LocalDateTime now = LocalDateTime.now();
        orderRepository.save(pendingOrder(5001L, now.plusHours(1)));

        List<Order> result = orderRepository.findAllByStatusAndOrderDeadlineBefore(OrderStatus.PENDING, now);

        assertThat(result).isEmpty();
    }

    private Order orderedOrder(Long auctionId, LocalDateTime completionDeadline) {
        Order order = pendingOrder(auctionId, LocalDateTime.now().plusHours(24));
        order.confirmOrder(
                DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호"),
                completionDeadline, LocalDateTime.now());
        return order;
    }

    @Test
    @DisplayName("거래 확정 기한이 지난 ORDERED 주문만 조회된다")
    void findAllByStatusAndCompletionDeadlineBefore_returnsOnlyExpiredOrderedOrders() {
        LocalDateTime now = LocalDateTime.now();
        Order expiredOrdered = orderRepository.save(orderedOrder(5001L, now.minusDays(1)));
        orderRepository.save(orderedOrder(5002L, now.plusDays(1)));
        orderRepository.save(pendingOrder(5003L, now.minusHours(1)));

        List<Order> result = orderRepository.findAllByStatusAndCompletionDeadlineBefore(OrderStatus.ORDERED, now);

        assertThat(result).extracting(Order::getId).containsExactly(expiredOrdered.getId());
    }

    @Test
    @DisplayName("대상이 없으면 빈 목록을 반환한다")
    void findAllByStatusAndCompletionDeadlineBefore_returnsEmptyWhenNoneExpired() {
        LocalDateTime now = LocalDateTime.now();
        orderRepository.save(orderedOrder(5001L, now.plusDays(1)));

        List<Order> result = orderRepository.findAllByStatusAndCompletionDeadlineBefore(OrderStatus.ORDERED, now);

        assertThat(result).isEmpty();
    }
}
