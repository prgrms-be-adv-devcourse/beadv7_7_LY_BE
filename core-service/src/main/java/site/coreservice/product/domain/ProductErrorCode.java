package site.coreservice.product.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

/**
 * 상품 도메인 에러코드. 채번은 노션 공통부분 규칙(상품 = PERR + 4000번대, GERR-0001과 같은 하이픈 표기).
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum ProductErrorCode implements ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PERR-4100", "상품을 찾을 수 없습니다"),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
