package site.memberservice.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import site.memberservice.member.exception.MemberException;
import site.memberservice.member.domain.Email;
import site.memberservice.util.NullAndBlankSource;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email VO")
class EmailTest {

    @DisplayName("유효한 이메일을 입력하면 Email 객체가 생성된다.")
    @Test
    void createEmail() {
        // Given
        final String input = "test@email.com";

        // When & Then
        assertThatCode(() -> new Email(input, "test-hash"))
            .doesNotThrowAnyException();
    }

    @DisplayName("null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputNullOrEmpty(final String input) {
        // When & Then
        assertThatThrownBy(() -> new Email(input, "test-hash"))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("회원 이메일은 null 혹은 공백일 수 없습니다. input : %s", input));
    }

    @DisplayName("유효하지 않은 형식의 이메일을 입력하면 예외가 발생한다.")
    @ValueSource(strings = {"test#email.com", "test.email.com현", "test@email..com"})
    @ParameterizedTest
    void throwExceptionWhenInputInvalidValue(final String input) {
        // When & Then
        assertThatThrownBy(() -> new Email(input, "test-hash"))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("유효하지 않은 형식의 이메일입니다. input : %s", input));
    }

    @DisplayName("hash가 null 혹은 공백이면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenHashNullOrEmpty(final String hash) {
        // When & Then
        assertThatThrownBy(() -> new Email("test@email.com", hash))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 이메일 해시는 null 혹은 공백일 수 없습니다.");
    }
}
