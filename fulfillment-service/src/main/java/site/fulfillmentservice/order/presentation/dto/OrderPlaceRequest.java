package site.fulfillmentservice.order.presentation.dto;

import site.fulfillmentservice.order.domain.DeliveryInfo;

public record OrderPlaceRequest(
        String recipientName,
        String phoneNumber,
        String baseAddress,
        String detailAddress
) {
    public DeliveryInfo toDeliveryInfo() {
        return DeliveryInfo.of(recipientName, phoneNumber, baseAddress, detailAddress);
    }
}
