package site.fulfillmentservice.order.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import site.fulfillmentservice.order.application.dto.DeliveryAddressResult;
import site.fulfillmentservice.order.application.dto.OrderDetailResult;
import site.fulfillmentservice.order.application.dto.OrderItemSnapshotResult;
import site.fulfillmentservice.order.application.dto.RefundInfoResult;

public record OrderDetailResponse(
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
        Product product,
        DeliveryAddress deliveryAddress,
        RefundInfo refundInfo
) {

    public static OrderDetailResponse from(OrderDetailResult result) {
        return new OrderDetailResponse(
                result.orderId(),
                result.auctionId(),
                result.buyerId(),
                result.sellerId(),
                result.finalBidPrice(),
                result.status(),
                result.cancelReason(),
                result.confirmationDeadline(),
                result.completionDeadline(),
                result.orderedAt(),
                result.completedAt(),
                result.cancelledAt(),
                Product.from(result.product()),
                DeliveryAddress.from(result.deliveryAddress()),
                RefundInfo.from(result.refundInfo())
        );
    }

    public record Product(
            Long productId,
            String artistName,
            String albumTitle,
            Integer releaseYear,
            String pressType,
            String conditionGrade,
            String coverImage
    ) {

        public static Product from(OrderItemSnapshotResult result) {
            return new Product(
                    result.productId(),
                    result.artistName(),
                    result.albumTitle(),
                    result.releaseYear(),
                    result.pressType(),
                    result.conditionGrade(),
                    result.coverImage()
            );
        }
    }

    public record DeliveryAddress(
            String recipientName,
            String phoneNumber,
            String baseAddress,
            String detailAddress
    ) {

        public static DeliveryAddress from(DeliveryAddressResult result) {
            if (result == null) {
                return null;
            }
            return new DeliveryAddress(
                    result.recipientName(),
                    result.phoneNumber(),
                    result.baseAddress(),
                    result.detailAddress()
            );
        }
    }

    public record RefundInfo(
            String reason,
            String description,
            List<String> imageUrls,
            LocalDateTime requestedAt,
            LocalDateTime refundedAt
    ) {

        public static RefundInfo from(RefundInfoResult result) {
            if (result == null) {
                return null;
            }
            return new RefundInfo(
                    result.reason(),
                    result.description(),
                    result.imageUrls(),
                    result.requestedAt(),
                    result.refundedAt()
            );
        }
    }
}
