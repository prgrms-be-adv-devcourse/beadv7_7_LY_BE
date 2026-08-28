package site.fulfillmentservice.order.application;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.common.event.contract.AuctionWonEvent;
import site.fulfillmentservice.order.application.dto.OrderDetailResult;
import site.fulfillmentservice.order.application.dto.OrderSearchResult;
import site.fulfillmentservice.order.application.dto.RefundRequestCommand;
import site.fulfillmentservice.order.domain.DeliveryInfo;
import site.fulfillmentservice.order.domain.Order;
import site.fulfillmentservice.order.domain.OrderItemSnapshot;
import site.fulfillmentservice.order.domain.OrderRepository;
import site.fulfillmentservice.order.domain.OrderSearchPage;
import site.fulfillmentservice.order.domain.OrderStatus;
import site.fulfillmentservice.order.application.port.ProductInfo;
import site.fulfillmentservice.order.application.port.ProductPort;
import site.fulfillmentservice.order.exception.OrderErrorCode;
import site.fulfillmentservice.order.exception.OrderException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final long ORDER_CONFIRMATION_HOURS = 24L;
    private static final long COMPLETION_PERIOD_DAYS = 7L;
    // 데드라인 직후 처리 중일 수 있는 사용자 요청과 겹치지 않도록, 스케줄러 폴링 주기(1분)만큼 늦춰서 조회한다.
    private static final long SCHEDULER_GRACE_PERIOD_MINUTES = 1L;
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

        order.cancelByBuyer(LocalDateTime.now());
        orderEventPublisher.publishCancelled(order);
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredOrderIds() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(SCHEDULER_GRACE_PERIOD_MINUTES);
        return orderRepository.findAllByStatusAndOrderDeadlineBefore(OrderStatus.PENDING, threshold).stream()
            .map(Order::getId)
            .toList();
    }

    public void cancelExpiredOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.isPending()) {
            return;
        }

        order.cancelByTimeout(LocalDateTime.now());
        orderEventPublisher.publishCancelled(order);
        log.info("주문 확정 기한 초과로 자동 취소 처리: orderId={}, auctionId={}", order.getId(), order.getAuctionId());
    }

    public void completeOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        order.completeByBuyer(LocalDateTime.now());
        orderEventPublisher.publishCompleted(order);
    }

    @Transactional(readOnly = true)
    public List<Long> findOrdersToAutoComplete() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(SCHEDULER_GRACE_PERIOD_MINUTES);
        return orderRepository.findAllByStatusAndCompletionDeadlineBefore(OrderStatus.ORDERED, threshold).stream()
            .map(Order::getId)
            .toList();
    }

    public void completeExpiredOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.isOrdered()) {
            return;
        }

        order.completeByTimeout(LocalDateTime.now());
        orderEventPublisher.publishCompleted(order);
        log.info("거래 확정 기한 초과로 자동 완료 처리: orderId={}, auctionId={}", order.getId(), order.getAuctionId());
    }

    public void requestRefund(Long orderId, Long buyerId, RefundRequestCommand command) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        order.requestRefund(command.reason(), command.description(), command.imageUrls(), LocalDateTime.now());
    }

    public void approveRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        order.approveRefund(LocalDateTime.now());
        orderEventPublisher.publishRefunded(order);
    }

    public void rejectRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        order.rejectRefund(LocalDateTime.now());
        orderEventPublisher.publishCompleted(order);
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
    public OrderDetailResult getOrderDetailForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

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

    @Transactional(readOnly = true)
    public OrderSearchResult findOrdersForAdmin(String rawStatus, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clampSize(size);
        OrderStatus status = parseStatus(rawStatus);

        OrderSearchPage searchPage = orderRepository.findAllByStatus(status, safePage, safeSize);

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
