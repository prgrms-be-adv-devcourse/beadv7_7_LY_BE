package site.fulfillmentservice.settlement.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SettlementErrorCode implements ErrorCode {

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "SERR-7001", "from은 to보다 늦을 수 없습니다."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "SERR-7002", "유효하지 않은 정산 상태입니다."),
    SETTLEMENT_ITEM_NOT_PENDING(HttpStatus.CONFLICT, "SERR-7003", "PENDING 상태의 정산 항목만 확정 처리할 수 있습니다."),
    COMMISSION_POLICY_ALREADY_CLOSED(HttpStatus.CONFLICT, "SERR-7004", "이미 종료된 정책입니다."),
    INVALID_EFFECTIVE_PERIOD(HttpStatus.BAD_REQUEST, "SERR-7005", "effectiveFrom은 effectiveTo보다 이전이어야 합니다."),
    INVALID_COMMISSION_RATE(HttpStatus.BAD_REQUEST, "SERR-7006", "commissionRate는 0 이상 1 미만이어야 합니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
