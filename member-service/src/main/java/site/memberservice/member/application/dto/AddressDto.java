package site.memberservice.member.application.dto;

import site.memberservice.member.domain.Address;

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
}
