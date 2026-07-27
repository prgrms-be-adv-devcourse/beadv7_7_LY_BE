package site.memberservice.auth.domain;

import lombok.Getter;
import site.memberservice.auth.exception.AuthException;

import java.util.Objects;

import static java.lang.String.*;
import static site.memberservice.auth.exception.AuthErrorCode.INVALID_AUTH_TOKEN;

@Getter
public class AuthToken {

    private final String value;

    public AuthToken(final String value) {
        validateValue(value);
        this.value = value;
    }

    private void validateValue(final String value) {
        if (value == null || value.isBlank()) {
            throw new AuthException(INVALID_AUTH_TOKEN, format("인증 토큰은 null 혹은 공백일 수 없습니다. input: %s", value));
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final AuthToken authToken = (AuthToken) o;
        return Objects.equals(value, authToken.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
