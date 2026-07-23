package site.coreservice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "orders")
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


    public void confirmOrder(DeliveryInfo deliveryInfo, LocalDateTime completionDeadline) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 주문할 수 있습니다. 현재 상태: " + status);
        }
        if (deliveryInfo == null) {
            throw new IllegalArgumentException("배송지 정보는 필수입니다.");
        }
        this.status = OrderStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();
        this.completionDeadline = completionDeadline;
        this.deliveryInfo = deliveryInfo;
    }

    public void cancelOrder(CancelReason cancelReason) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 취소할 수 있습니다. 현재 상태: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = cancelReason;
    }

    public void completeOrder() {
        if (status != OrderStatus.ORDERED) {
            throw new IllegalStateException("ORDERED 상태에서만 거래를 확정할 수 있습니다. 현재 상태: " + status);
        }
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
