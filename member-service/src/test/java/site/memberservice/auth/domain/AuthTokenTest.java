package site.memberservice.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import site.memberservice.auth.exception.AuthException;
import site.memberservice.util.NullAndBlankSource;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthToken VO")
class AuthTokenTest {

    @DisplayName("유효한 인증 토큰을 입력하면 AuthToken 객체가 생성된다.")
    @Test
    void createAuthToken() {
        // Given
        final String input = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY" +
            "3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTA" +
            "yMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";

        // When & Then
        assertThatCode(() -> new AuthToken(input))
            .doesNotThrowAnyException();
    }

    @DisplayName("null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputNullOrEmpty(final String input) {
        // When & Then
        assertThatThrownBy(() -> new AuthToken(input))
            .isInstanceOf(AuthException.class)
            .hasMessage(format("인증 토큰은 null 혹은 공백일 수 없습니다. input: %s", input));
    }
}
