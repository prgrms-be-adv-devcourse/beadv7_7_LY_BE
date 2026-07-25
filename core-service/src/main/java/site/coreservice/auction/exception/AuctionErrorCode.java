package site.coreservice.auction.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum AuctionErrorCode implements ErrorCode {
    ITEM_CONDITION_INVALID(HttpStatus.BAD_REQUEST, "AERR-5001", "유효하지 않은 상품 상태입니다."),
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "AERR-5002", "경매를 찾을 수 없습니다."),
    AUCTION_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "AERR-5003", "변경할 수 없는 경매 상태입니다."),
    AUCTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AERR-5004", "해당 경매에 대한 권한이 없습니다."),
    AUCTION_SEARCH_VIEW_NOT_FOUND(HttpStatus.BAD_REQUEST, "AERR-5005", "경매 서치 뷰가 존재하지 존재하지 않습니다."),
    BID_NOT_FOUND(HttpStatus.NOT_FOUND, "AERR-5006", "입찰을 찾을 수 없습니다."),
    AUCTION_STATUS_INVALID(HttpStatus.BAD_REQUEST, "AERR-5007", "유효하지 않은 경매 상태입니다."),
    AUCTION_SORT_INVALID(HttpStatus.BAD_REQUEST, "AERR-5008", "유효하지 않은 정렬 조건입니다."),
    WATCHED_AUCTION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "AERR-5101", "장바구니 경매 개수 제한을 초과했습니다."),

    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}