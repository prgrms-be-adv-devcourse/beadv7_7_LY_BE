package site.coreservice.order.application;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.coreservice.auction.domain.AuctionWonEvent;
import site.coreservice.order.domain.ConditionGrade;
import site.coreservice.order.domain.DeliveryInfo;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderItemSnapshot;
import site.coreservice.order.domain.OrderRepository;
import site.coreservice.order.application.port.ProductInfo;
import site.coreservice.order.application.port.ProductPort;
import site.coreservice.order.exception.OrderErrorCode;
import site.coreservice.order.exception.OrderException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final long ORDER_CONFIRMATION_HOURS = 24L;
    private static final long COMPLETION_PERIOD_DAYS = 7L;

    private final OrderRepository orderRepository;
    private final ProductPort productPort;

    public void createOrder(AuctionWonEvent event) {
        if (orderRepository.existsByAuctionId(event.getAuctionId())) {
            log.info("이미 주문이 생성된 낙찰 이벤트입니다. 중복 처리로 건너뜁니다. auctionId={}", event.getAuctionId());
            return;
        }

        ProductInfo productInfo = productPort.getProductInfo(event.getProductId());

        OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
            productInfo.title(),
            productInfo.artistName(),
            productInfo.releaseYear(),
            productInfo.pressType(),
            ConditionGrade.valueOf(event.getItemCondition()),
            event.getFirstImageUrl()
        );

        LocalDateTime orderDeadline = LocalDateTime.now().plusHours(ORDER_CONFIRMATION_HOURS);

        Order order = Order.of(
            event.getAuctionId(),
            event.getProductId(),
            event.getWinnerId(),
            event.getSellerId(),
            event.getWinningPrice(),
            orderDeadline,
            itemSnapshot
        );

        try {
            orderRepository.save(order);
        } catch (DataIntegrityViolationException e) {
            log.info("이미 주문이 생성된 낙찰 이벤트입니다(동시성). 중복 처리로 건너뜁니다. auctionId={}", event.getAuctionId());
        }
    }

    public void placeOrder(Long orderId, Long buyerId, DeliveryInfo deliveryInfo) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        if (deliveryInfo == null || !StringUtils.hasText(deliveryInfo.getBaseAddress())) {
            throw new OrderException(OrderErrorCode.ADDRESS_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        order.confirmOrder(deliveryInfo, now.plusDays(COMPLETION_PERIOD_DAYS), now);
    }
}
