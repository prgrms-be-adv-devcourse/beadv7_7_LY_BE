package site.fulfillmentservice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;
import site.fulfillmentservice.order.exception.OrderErrorCode;
import site.fulfillmentservice.order.exception.OrderException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(name = "ukOrderAuctionId", columnNames = "auction_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "final_bid_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalBidPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    private CancelReason cancelReason;

    @Column(name = "order_deadline", nullable = false)
    private LocalDateTime orderDeadline;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "completion_deadline")
    private LocalDateTime completionDeadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Embedded
    private DeliveryInfo deliveryInfo;

    @Embedded
    private OrderItemSnapshot itemSnapshot;

    @Embedded
    private RefundInfo refundInfo;

    private Order(Long auctionId, Long productId, Long buyerId, Long sellerId, BigDecimal finalBidPrice,
            LocalDateTime orderDeadline, OrderItemSnapshot itemSnapshot) {
        validate(auctionId, productId, buyerId, sellerId, finalBidPrice, orderDeadline, itemSnapshot);
        this.auctionId = auctionId;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.finalBidPrice = finalBidPrice;
        this.status = OrderStatus.PENDING;
        this.orderDeadline = orderDeadline;
        this.itemSnapshot = itemSnapshot;
    }

    public static Order of(Long auctionId, Long productId, Long buyerId, Long sellerId, BigDecimal finalBidPrice,
            LocalDateTime orderDeadline, OrderItemSnapshot itemSnapshot) {
        return new Order(auctionId, productId, buyerId, sellerId, finalBidPrice, orderDeadline, itemSnapshot);
    }

    private static void validate(Long auctionId, Long productId, Long buyerId, Long sellerId,
            BigDecimal finalBidPrice, LocalDateTime orderDeadline, OrderItemSnapshot itemSnapshot) {
        Objects.requireNonNull(auctionId, "auctionId는 null일 수 없습니다.");
        Objects.requireNonNull(productId, "productId는 null일 수 없습니다.");
        Objects.requireNonNull(buyerId, "buyerId는 null일 수 없습니다.");
        Objects.requireNonNull(sellerId, "sellerId는 null일 수 없습니다.");
        Objects.requireNonNull(finalBidPrice, "finalBidPrice는 null일 수 없습니다.");
        Objects.requireNonNull(orderDeadline, "orderDeadline은 null일 수 없습니다.");
        Objects.requireNonNull(itemSnapshot, "itemSnapshot은 null일 수 없습니다.");
    }


    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isOrdered() {
        return status == OrderStatus.ORDERED;
    }

    public void confirmOrder(DeliveryInfo deliveryInfo, LocalDateTime completionDeadline, LocalDateTime now) {
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        if (!isPending()) {
            throw new OrderException(OrderErrorCode.ORDER_NOT_PENDING);
        }
        if (deliveryInfo == null) {
            throw new IllegalArgumentException("배송지 정보는 필수입니다.");
        }
        if (now.isAfter(orderDeadline)) {
            throw new OrderException(OrderErrorCode.ORDER_DEADLINE_EXPIRED);
        }
        this.status = OrderStatus.ORDERED;
        this.orderedAt = now;
        this.completionDeadline = completionDeadline;
        this.deliveryInfo = deliveryInfo;
    }

    public void cancelByBuyer(LocalDateTime now) {
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        if (!isPending()) {
            throw new OrderException(OrderErrorCode.ORDER_NOT_CANCELLABLE);
        }
        if (now.isAfter(orderDeadline)) {
            throw new OrderException(OrderErrorCode.ORDER_CANCEL_DEADLINE_EXPIRED);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelReason = CancelReason.BUYER_DECLINED;
    }

    public void cancelByTimeout(LocalDateTime now) {
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        if (!isPending()) {
            throw new OrderException(OrderErrorCode.ORDER_NOT_CANCELLABLE);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelReason = CancelReason.CONFIRMATION_TIMEOUT;
    }

    public void completeByBuyer(LocalDateTime now) {
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        if (!isOrdered()) {
            throw new OrderException(OrderErrorCode.ORDER_NOT_ORDERED);
        }
        if (now.isAfter(completionDeadline)) {
            throw new OrderException(OrderErrorCode.COMPLETION_DEADLINE_EXPIRED);
        }
        this.status = OrderStatus.COMPLETED;
        this.completedAt = now;
    }

    public void completeByTimeout(LocalDateTime now) {
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        if (!isOrdered()) {
            throw new OrderException(OrderErrorCode.ORDER_NOT_ORDERED);
        }
        this.status = OrderStatus.COMPLETED;
        this.completedAt = now;
    }

    public void requestRefund(RefundReason reason, String description, List<String> imageUrls, LocalDateTime now) {
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        if (!isOrdered()) {
            throw new OrderException(OrderErrorCode.ORDER_NOT_REFUNDABLE);
        }
        if (now.isAfter(completionDeadline)) {
            throw new OrderException(OrderErrorCode.REFUND_REQUEST_DEADLINE_EXPIRED);
        }
        this.status = OrderStatus.REFUND_REQUESTED;
        this.refundInfo = RefundInfo.request(reason, description, imageUrls, now);
    }

    public void approveRefund(LocalDateTime now) {
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        if (status != OrderStatus.REFUND_REQUESTED) {
            throw new OrderException(OrderErrorCode.REFUND_NOT_REQUESTED);
        }
        this.status = OrderStatus.REFUND;
        this.refundInfo = this.refundInfo.refund(now);
    }

    public void rejectRefund(LocalDateTime now) {
        Objects.requireNonNull(now, "now는 null일 수 없습니다.");
        if (status != OrderStatus.REFUND_REQUESTED) {
            throw new OrderException(OrderErrorCode.REFUND_NOT_REQUESTED);
        }
        this.status = OrderStatus.REFUND_REJECTED;
        this.completedAt = now;
    }
}
