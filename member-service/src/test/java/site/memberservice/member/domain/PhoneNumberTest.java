package site.memberservice.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import site.memberservice.member.exception.MemberException;
import site.memberservice.member.domain.PhoneNumber;
import site.memberservice.util.NullAndBlankSource;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PhoneNumber VO")
class PhoneNumberTest {

    @DisplayName("유효한 전화번호를 입력하면 PhoneNumber 객체가 생성된다.")
    @Test
    void createPhoneNumber() {
        // Given
        final String input = "010-1234-5678";

        // When & Then
        assertThatCode(() -> new PhoneNumber(input))
            .doesNotThrowAnyException();
    }

    @DisplayName("null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputNullOrEmpty(final String input) {
        // When & Then
        assertThatThrownBy(() -> new PhoneNumber(input))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("회원 전화번호는 null 혹은 공백일 수 없습니다. input : %s", input));
    }

    @DisplayName("유효하지 않은 형식의 전화번호를 입력하면 예외가 발생한다.")
    @ValueSource(strings = {"01012345678", "010.1234.5678", "1234-5678"})
    @ParameterizedTest
    void throwExceptionWhenInputInvalidValue(final String input) {
        // When & Then
        assertThatThrownBy(() -> new PhoneNumber(input))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("유효하지 않은 형식의 전화번호입니다. input : %s", input));
    }
}
