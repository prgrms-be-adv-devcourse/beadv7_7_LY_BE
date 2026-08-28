package site.fulfillmentservice.order.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import site.fulfillmentservice.order.domain.RefundInfo;

public record RefundInfoResult(
        String reason,
        String description,
        List<String> imageUrls,
        LocalDateTime requestedAt,
        LocalDateTime refundedAt
) {

    public static RefundInfoResult from(RefundInfo refundInfo) {
        if (refundInfo == null) {
            return null;
        }
        return new RefundInfoResult(
                refundInfo.getReason().name(),
                refundInfo.getDescription(),
                refundInfo.getImageUrls(),
                refundInfo.getRequestedAt(),
                refundInfo.getRefundedAt()
        );
    }
}
