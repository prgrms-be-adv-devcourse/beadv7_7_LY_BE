package site.memberservice.auth.exception;

import static site.memberservice.auth.exception.AuthErrorCode.INVALID_AUTH_TOKEN;

public class RefreshTokenReuseDetectedException extends AuthException {
    public RefreshTokenReuseDetectedException(final String message) {
        super(INVALID_AUTH_TOKEN, message);
    }
}
