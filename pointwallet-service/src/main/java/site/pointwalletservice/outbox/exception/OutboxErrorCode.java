// outbox/exception/OutboxErrorCode.java
package site.pointwalletservice.outbox.exception;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum OutboxErrorCode implements ErrorCode {

    OUTBOX_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "OBERR-3001", "존재하지 않는 outbox 이벤트입니다."),
    INVALID_STATUS_FOR_RETRY(HttpStatus.CONFLICT, "OBERR-3002", "DEAD 상태의 이벤트만 수동 재시도할 수 있습니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}