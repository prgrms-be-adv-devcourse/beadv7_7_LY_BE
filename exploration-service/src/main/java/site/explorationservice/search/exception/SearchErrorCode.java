package site.explorationservice.search.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SearchErrorCode implements ErrorCode {

    /**
     * 코드 값이 상품 서비스의 것과 같다. 검색을 이 서비스로 옮기는 게 목적이지 응답 계약을 바꾸는 게 아니라서,
     * 프론트가 코드로 분기하고 있어도 그대로 동작해야 한다.
     */
    SEARCH_KEYWORD_REQUIRED(HttpStatus.BAD_REQUEST, "PERR-4001", "검색어(q)는 필수입니다"),

    /** 상품 서비스에는 짝이 없는 코드다. 검색 대상 자체가 이 서비스에서 처음 생긴 개념이라 새 번호를 뗀다. */
    SEARCH_TARGET_UNSUPPORTED(HttpStatus.BAD_REQUEST, "PERR-4002", "지원하지 않는 검색 대상입니다"),

    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
