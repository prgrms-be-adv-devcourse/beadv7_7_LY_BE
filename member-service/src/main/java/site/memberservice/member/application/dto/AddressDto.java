package site.memberservice.member.application.dto;

import site.memberservice.member.domain.Address;
import site.memberservice.member.domain.repository.MemberAddressView;

public record AddressDto(
    String zipcode,
    String baseAddress,
    String detailAddress
) {
    public static AddressDto from(final Address address) {
        return new AddressDto(
            address.getZipcode(),
            address.getBaseAddress(),
            address.getDetailAddress()
        );
    }

    public static AddressDto from(final MemberAddressView view) {
        return new AddressDto(view.zipcode(), view.baseAddress(), view.detailAddress());
    }
}
