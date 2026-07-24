package site.memberservice.member.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum MemberErrorCode implements ErrorCode {

    INVALID_MEMBER_INFO(BAD_REQUEST, "MERR-0001", "유효하지 않은 회원 정보입니다.")
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
