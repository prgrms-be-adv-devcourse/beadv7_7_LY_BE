package site.coreservice.order.application;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.common.event.contract.AuctionWonEvent;
import site.coreservice.order.application.dto.OrderDetailResult;
import site.coreservice.order.application.dto.OrderSearchResult;
import site.coreservice.order.domain.CancelReason;
import site.coreservice.order.domain.DeliveryInfo;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderItemSnapshot;
import site.coreservice.order.domain.OrderRepository;
import site.coreservice.order.domain.OrderSearchPage;
import site.coreservice.order.domain.OrderStatus;
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
    private static final int MIN_SIZE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final OrderRepository orderRepository;
    private final ProductPort productPort;
    private final OrderEventPublisher orderEventPublisher;

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
            event.getItemCondition(),
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
            log.warn("이미 주문이 생성된 낙찰 이벤트입니다(동시성). 중복 처리로 건너뜁니다. auctionId={}", event.getAuctionId());
        }
    }

    public void placeOrder(Long orderId, Long buyerId, DeliveryInfo deliveryInfo) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        if (deliveryInfo == null) {
            throw new OrderException(OrderErrorCode.ADDRESS_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        order.confirmOrder(deliveryInfo, now.plusDays(COMPLETION_PERIOD_DAYS), now);
    }

    public void cancelOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        order.cancelOrder(CancelReason.BUYER_DECLINED, LocalDateTime.now());
        orderEventPublisher.publishCancelled(order);
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredOrderIds() {
        return orderRepository.findAllByStatusAndOrderDeadlineBefore(OrderStatus.PENDING, LocalDateTime.now()).stream()
            .map(Order::getId)
            .toList();
    }

    public void cancelExpiredOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        order.cancelOrder(CancelReason.CONFIRMATION_TIMEOUT, LocalDateTime.now());
        orderEventPublisher.publishCancelled(order);
        log.info("주문 확정 기한 초과로 자동 취소 처리: orderId={}, auctionId={}", order.getId(), order.getAuctionId());
    }

    public void completeOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        order.completeOrder(LocalDateTime.now());
        orderEventPublisher.publishCompleted(order);
    }

    @Transactional(readOnly = true)
    public List<Long> findOrdersToAutoComplete() {
        return orderRepository.findAllByStatusAndCompletionDeadlineBefore(OrderStatus.ORDERED, LocalDateTime.now()).stream()
            .map(Order::getId)
            .toList();
    }

    public void completeExpiredOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.ORDERED) {
            return;
        }

        order.completeOrder(LocalDateTime.now());
        orderEventPublisher.publishCompleted(order);
        log.info("거래 확정 기한 초과로 자동 완료 처리: orderId={}, auctionId={}", order.getId(), order.getAuctionId());
    }

    @Transactional(readOnly = true)
    public OrderDetailResult getOrderDetail(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyerId().equals(memberId) && !order.getSellerId().equals(memberId)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        return OrderDetailResult.from(order);
    }

    @Transactional(readOnly = true)
    public OrderSearchResult findOrders(Long memberId, String perspective, String rawStatus, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clampSize(size);
        OrderStatus status = parseStatus(rawStatus);

        OrderSearchPage searchPage = switch (perspective == null ? "" : perspective.toLowerCase()) {
            case "buyer" -> orderRepository.findAllByBuyerId(memberId, status, safePage, safeSize);
            case "seller" -> orderRepository.findAllBySellerId(memberId, status, safePage, safeSize);
            default -> throw new OrderException(OrderErrorCode.INVALID_PERSPECTIVE);
        };

        return OrderSearchResult.of(searchPage, safePage, safeSize);
    }

    private OrderStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new OrderException(OrderErrorCode.INVALID_STATUS);
        }
    }

    private int clampSize(int size) {
        if (size < MIN_SIZE) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
