package site.coreservice.order.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.coreservice.order.domain.Order;

public record OrderSummaryResult(
        Long orderId,
        Long auctionId,
        String status,
        BigDecimal finalBidPrice,
        LocalDateTime confirmationDeadline,
        LocalDateTime completionDeadline,
        OrderItemSnapshotResult product
) {

    public static OrderSummaryResult from(Order order) {
        return new OrderSummaryResult(
                order.getId(),
                order.getAuctionId(),
                order.getStatus().name(),
                order.getFinalBidPrice(),
                order.getOrderDeadline(),
                order.getCompletionDeadline(),
                OrderItemSnapshotResult.from(order.getProductId(), order.getItemSnapshot())
        );
    }
}
