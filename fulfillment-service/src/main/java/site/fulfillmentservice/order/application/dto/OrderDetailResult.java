package site.fulfillmentservice.order.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.fulfillmentservice.order.domain.Order;

public record OrderDetailResult(
        Long orderId,
        Long auctionId,
        Long buyerId,
        Long sellerId,
        BigDecimal finalBidPrice,
        String status,
        String cancelReason,
        LocalDateTime confirmationDeadline,
        LocalDateTime completionDeadline,
        LocalDateTime orderedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        OrderItemSnapshotResult product,
        DeliveryAddressResult deliveryAddress,
        RefundInfoResult refundInfo
) {

    public static OrderDetailResult from(Order order) {
        return new OrderDetailResult(
                order.getId(),
                order.getAuctionId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getFinalBidPrice(),
                order.getStatus().name(),
                order.getCancelReason() == null ? null : order.getCancelReason().name(),
                order.getOrderDeadline(),
                order.getCompletionDeadline(),
                order.getOrderedAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                OrderItemSnapshotResult.from(order.getProductId(), order.getItemSnapshot()),
                DeliveryAddressResult.from(order.getDeliveryInfo()),
                RefundInfoResult.from(order.getRefundInfo())
        );
    }
}
