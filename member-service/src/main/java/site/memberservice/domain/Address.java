package site.memberservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.memberservice.exception.MemberException;

import java.util.Objects;

import static java.lang.String.format;
import static site.memberservice.exception.MemberErrorCode.INVALID_MEMBER_INFO;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Address {

    @Column(name = "zipcode", length = 10, nullable = false)
    private String zipcode;

    @Column(name = "base_address", length = 255, nullable = false)
    private String baseAddress;

    @Column(name = "detail_address", length = 255, nullable = false)
    private String detailAddress;

    public Address(
        final String zipcode,
        final String baseAddress,
        final String detailAddress
    ) {
        validateZipcode(zipcode);
        validateBaseAddress(baseAddress);
        validateDetailAddress(detailAddress);

        this.zipcode = zipcode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }

    private void validateZipcode(final String zipcode) {
        if (zipcode == null || zipcode.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("우편번호는 null 혹은 공백일 수 없습니다. input: %s", zipcode));
        }
    }

    private void validateBaseAddress(final String baseAddress) {
        if (baseAddress == null || baseAddress.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("기본 주소는 null 혹은 공백일 수 없습니다. input: %s", baseAddress));
        }
    }

    private void validateDetailAddress(final String detailAddress) {
        if (detailAddress == null || detailAddress.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("상세 주소는 null 혹은 공백일 수 없습니다. input: %s", detailAddress));
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final Address address = (Address) o;
        return Objects.equals(zipcode, address.zipcode) &&
            Objects.equals(baseAddress, address.baseAddress) &&
            Objects.equals(detailAddress, address.detailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipcode, baseAddress, detailAddress);
    }
}
