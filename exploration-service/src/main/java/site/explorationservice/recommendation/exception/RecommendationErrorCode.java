package site.explorationservice.recommendation.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum RecommendationErrorCode implements ErrorCode {

    WISHLIST_LOOKUP_FAILED(HttpStatus.BAD_GATEWAY, "RERR-5001", "위시리스트 조회에 실패했습니다."),
    WISHLIST_REQUEST_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "RERR-5002",
        "위시리스트 조회 요청이 올바르지 않습니다."),

    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
