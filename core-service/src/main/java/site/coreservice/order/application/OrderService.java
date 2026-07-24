package site.coreservice.order.application;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.coreservice.auction.domain.AuctionWonEvent;
import site.coreservice.order.domain.ConditionGrade;
import site.coreservice.order.domain.DeliveryInfo;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderItemSnapshot;
import site.coreservice.order.domain.OrderRepository;
import site.coreservice.order.exception.OrderErrorCode;
import site.coreservice.order.exception.OrderException;
import site.coreservice.product.application.ProductService;
import site.coreservice.product.application.dto.ProductSnapshotResult;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final long ORDER_CONFIRMATION_HOURS = 24L;
    private static final long COMPLETION_PERIOD_DAYS = 7L;

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public void createOrder(final AuctionWonEvent event) {
        if (orderRepository.existsByAuctionId(event.getAuctionId())) {
            log.info("이미 주문이 생성된 낙찰 이벤트입니다. 중복 처리로 건너뜁니다. auctionId={}", event.getAuctionId());
            return;
        }

        final ProductSnapshotResult productSnapshot = productService.getProductSnapshot(event.getProductId());

        final OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
            productSnapshot.title(),
            productSnapshot.artistName(),
            productSnapshot.releaseYear(),
            productSnapshot.pressType().name(),
            ConditionGrade.valueOf(event.getItemCondition()),
            event.getFirstImageUrl()
        );

        final LocalDateTime orderDeadline = LocalDateTime.now().plusHours(ORDER_CONFIRMATION_HOURS);

        final Order order = Order.of(
            event.getAuctionId(),
            event.getProductId(),
            event.getWinnerId(),
            event.getSellerId(),
            event.getWinningPrice(),
            orderDeadline,
            itemSnapshot
        );

        orderRepository.save(order);
    }

    public void placeOrder(final Long orderId, final Long buyerId, final DeliveryInfo deliveryInfo) {
        final Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        if (deliveryInfo == null || !StringUtils.hasText(deliveryInfo.getBaseAddress())) {
            throw new OrderException(OrderErrorCode.ADDRESS_REQUIRED);
        }

        final LocalDateTime now = LocalDateTime.now();
        order.confirmOrder(deliveryInfo, now.plusDays(COMPLETION_PERIOD_DAYS), now);
    }
}
