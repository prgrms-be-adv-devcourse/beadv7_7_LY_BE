package site.memberservice.member.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class MemberException extends BusinessException {

    public MemberException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public MemberException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
}
