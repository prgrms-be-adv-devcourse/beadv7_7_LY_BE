package site.fulfillmentservice.order.application.dto;

import java.util.List;
import site.fulfillmentservice.order.domain.RefundReason;

public record RefundRequestCommand(
        RefundReason reason,
        String description,
        List<String> imageUrls
) {
}
