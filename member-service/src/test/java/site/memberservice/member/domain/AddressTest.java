package site.memberservice.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import site.memberservice.member.exception.MemberException;
import site.memberservice.member.domain.Address;
import site.memberservice.util.NullAndBlankSource;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Address VO")
class AddressTest {

    @DisplayName("유효한 주소를 입력하면 Address 객체가 생성된다.")
    @Test
    void createAddress() {
        // Given
        final String zipcode = "06671";
        final String baseAddress = "서울특별시 서초구 반포대로 45";
        final String detailAddress = "4층(서초동, 명정빌딩)";

        // When & Then
        assertThatCode(() -> new Address(zipcode, baseAddress, detailAddress))
            .doesNotThrowAnyException();
    }

    @DisplayName("zipcode로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputZipCodeNullOrEmpty(final String zipcode) {
        // Given
        final String baseAddress = "서울특별시 서초구 반포대로 45";
        final String detailAddress = "4층(서초동, 명정빌딩)";

        // When & Then
        assertThatThrownBy(() -> new Address(zipcode, baseAddress, detailAddress))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("우편번호는 null 혹은 공백일 수 없습니다. input: %s", zipcode));
    }

    @DisplayName("baseAddress로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputBaseAddressNullOrEmpty(final String baseAddress) {
        // Given
        final String zipcode = "06671";
        final String detailAddress = "4층(서초동, 명정빌딩)";

        // When & Then
        assertThatThrownBy(() -> new Address(zipcode, baseAddress, detailAddress))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("기본 주소는 null 혹은 공백일 수 없습니다. input: %s", baseAddress));
    }

    @DisplayName("detailAddress로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputDetailAddressNullOrEmpty(final String detailAddress) {
        // Given
        final String zipcode = "06671";
        final String baseAddress = "서울특별시 서초구 반포대로 45";

        // When & Then
        assertThatThrownBy(() -> new Address(zipcode, baseAddress, detailAddress))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("상세 주소는 null 혹은 공백일 수 없습니다. input: %s", detailAddress));
    }
}
