package site.auctionservice.exception;

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
    AUCTION_NOT_BIDDABLE(HttpStatus.CONFLICT , "AERR-5009", "입찰할 수 없는 경매 상태입니다."),
    AUCTION_SELLER_CANNOT_BID(HttpStatus.FORBIDDEN , "AERR-5010", "경매 판매자는 자신이 생성한 경매를 입찰할 수 없습니다."),
    ALREADY_HIGHEST_BIDDER(HttpStatus.FORBIDDEN ,"AERR-5011", "현재 최고 입찰자는 다시 입찰할 수 없습니다."),
    BID_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "AERR-5012", "입찰 금액이 최소 입찰가보다 낮습니다."),
    WALLET_HOLD_FAILED(HttpStatus.BAD_GATEWAY, "AERR-5013", "예치금 처리 중 오류가 발생했습니다."),
    AUCTION_START_TOO_SOON(HttpStatus.BAD_REQUEST, "AERR-5014", "경매 시작 시각은 현재로부터 최소 30분 이후여야 합니다."),
    WATCHED_AUCTION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "AERR-5101", "장바구니 경매 개수 제한을 초과했습니다."),
    WATCHED_AUCTION_STATUS_INVALID(HttpStatus.BAD_REQUEST, "AERR-5102",
        "종료되었거나 취소된 경매는 장바구니에 등록할 수 없습니다."),

    UPSTREAM_CONTRACT_VIOLATION(HttpStatus.INTERNAL_SERVER_ERROR, "AERR-5015", "연동 서비스 응답이 기대한 형태와 다릅니다."),
    PRODUCT_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "AERR-5016", "판매 중단된 상품으로는 경매를 등록할 수 없습니다."),
    WALLET_NOT_FOUND(HttpStatus.BAD_GATEWAY, "AERR-5017", "지갑을 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_CONTENT, "AERR-5018", "예치금이 부족합니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "AERR-5019", "요청이 몰려 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    WALLET_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AERR-5020", "지갑 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요."),

    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}