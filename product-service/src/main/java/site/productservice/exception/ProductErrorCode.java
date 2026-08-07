package site.productservice.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

/**
 * 상품 도메인 에러코드. 채번은 공통부분 규칙(상품 = PERR + 4000번대, 하이픈 표기 PERR-####).
 * <p>
 * 대역 구분:
 * <ul>
 * <li>40xx 요청 오류 — 사용자 입력이 잘못됨 (400). 4000 형식/범위, 4001 필수 누락</li>
 * <li>41xx 상품 리소스 오류 — 상품의 부재·상태 (주로 404). 4100 없음, 4101 비활성, 4102 병합됨</li>
 * <li>42xx 아티스트 리소스 오류 — 4200 없음, 4201 병합 대상 오류</li>
 * <li>49xx 내부 서버 오류 — 사용자 입력이 아닌 내부·이벤트 경로에서 정합성이 깨진 경우 (500)</li>
 * </ul>
 * 지금 실제로 쓰는 코드만 등록한다 — 계획 대역(4101 등)은 해당 기능이 생길 때 추가.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum ProductErrorCode implements ErrorCode {

    // 40xx 요청 오류
    SEARCH_KEYWORD_REQUIRED(HttpStatus.BAD_REQUEST, "PERR-4001", "검색어(q)는 필수입니다"),

    // 41xx 상품 리소스 오류
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PERR-4100", "상품을 찾을 수 없습니다"),

    // 49xx 내부 서버 오류 (정합성 깨짐)
    PRICE_HISTORY_AUCTION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "PERR-4901",
            "시세를 적재할 경매를 찾을 수 없습니다"),
    PRICE_HISTORY_AUCTION_NOT_CLOSED(HttpStatus.INTERNAL_SERVER_ERROR, "PERR-4902",
            "거래확정 이벤트인데 경매가 마감 상태가 아닙니다"),
    AUCTION_CONTRACT_VIOLATION(HttpStatus.INTERNAL_SERVER_ERROR, "PERR-4903",
            "경매 조회 응답이 우리 계약과 다릅니다"),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
