package site.fulfillmentservice.order.exception;

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
    ORDER_NOT_PENDING(HttpStatus.CONFLICT, "OERR-2004", "PENDING 상태의 주문만 확정할 수 있습니다"),
    ORDER_DEADLINE_EXPIRED(HttpStatus.CONFLICT, "OERR-2005", "주문 확정 기한이 지났습니다"),
    ORDER_NOT_CANCELLABLE(HttpStatus.CONFLICT, "OERR-2006", "취소할 수 없는 주문 상태입니다"),
    ORDER_NOT_ORDERED(HttpStatus.CONFLICT, "OERR-2007", "ORDERED 상태의 주문만 거래 확정할 수 있습니다"),
    INVALID_PERSPECTIVE(HttpStatus.BAD_REQUEST, "OERR-2008", "perspective는 buyer 또는 seller여야 합니다"),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "OERR-2009", "유효하지 않은 주문 상태입니다"),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
