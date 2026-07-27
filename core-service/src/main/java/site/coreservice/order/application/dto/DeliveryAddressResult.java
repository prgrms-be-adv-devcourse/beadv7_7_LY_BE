package site.coreservice.order.application.dto;

import site.coreservice.order.domain.DeliveryInfo;

public record DeliveryAddressResult(String recipientName, String phoneNumber, String baseAddress, String detailAddress) {

    public static DeliveryAddressResult from(DeliveryInfo deliveryInfo) {
        if (deliveryInfo == null) {
            return null;
        }
        return new DeliveryAddressResult(
                deliveryInfo.getRecipientName(),
                deliveryInfo.getPhoneNumber(),
                deliveryInfo.getBaseAddress(),
                deliveryInfo.getDetailAddress()
        );
    }
}
