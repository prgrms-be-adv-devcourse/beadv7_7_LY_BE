package site.coreservice.auction.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum AuctionErrorCode implements ErrorCode {
    ITEM_CONDITION_INVALID(HttpStatus.BAD_REQUEST, "AERR-5001", "유효하지 않은 상품 상태입니다.")
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}