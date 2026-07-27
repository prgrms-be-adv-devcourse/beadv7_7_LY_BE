package site.memberservice.auth.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class AuthException extends BusinessException {
    public AuthException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
}
