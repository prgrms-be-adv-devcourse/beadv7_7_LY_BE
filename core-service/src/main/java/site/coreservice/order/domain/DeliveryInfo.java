package site.coreservice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryInfo {

    @Column(name = "delivery_recipient_name")
    private String recipientName;

    @Column(name = "delivery_phone_number")
    private String phoneNumber;

    @Column(name = "delivery_base_address")
    private String baseAddress;

    @Column(name = "delivery_detail_address")
    private String detailAddress;

    private DeliveryInfo(String recipientName, String phoneNumber, String baseAddress, String detailAddress) {
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }

    public static DeliveryInfo of(String recipientName, String phoneNumber, String baseAddress,
            String detailAddress) {
        return new DeliveryInfo(recipientName, phoneNumber, baseAddress, detailAddress);
    }
}
