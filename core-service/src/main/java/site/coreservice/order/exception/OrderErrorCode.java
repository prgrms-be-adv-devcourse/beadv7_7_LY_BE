package site.coreservice.order.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "OERR-2001", "주문을 찾을 수 없습니다"),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "OERR-2002", "본인의 주문만 처리할 수 있습니다"),
    ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "OERR-2003", "배송지 정보는 필수입니다"),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
