package site.fulfillmentservice.order.presentation.dto;

import java.util.List;
import site.fulfillmentservice.order.application.dto.RefundRequestCommand;
import site.fulfillmentservice.order.domain.RefundReason;

public record RefundRequest(
        RefundReason reason,
        String description,
        List<String> imageUrls
) {
    public RefundRequestCommand toCommand() {
        return new RefundRequestCommand(reason, description, imageUrls);
    }
}
