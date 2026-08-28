package site.explorationservice.searchlog.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

/**
 * 접두어는 검색 로그(Search Log)에서 땄다. 한 글자 접두어가 이미 정산과 원장에 배정돼 있어 두 글자로 늘렸고,
 * 출금 코드가 지갑과 겹쳐 같은 방식을 쓴 선례가 있다. 번호는 아직 아무도 쓰지 않는 대역이다.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SearchLogErrorCode implements ErrorCode {

    SEARCH_CLICK_INVALID(HttpStatus.BAD_REQUEST, "SLERR-6001",
        "클릭 기록 요청 값이 올바르지 않습니다"),

    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
