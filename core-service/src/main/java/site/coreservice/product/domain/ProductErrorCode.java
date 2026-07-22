package site.coreservice.product.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import site.common.exception.ErrorCode;

/**
 * 상품 도메인 에러코드. 채번은 노션 공통부분 규칙(상품 = PERR + 4000번대, GERR-0001과 같은 하이픈 표기).
 * TODO (#48) : common ErrorCode에 HttpStatus getStatus()가 추가되면 status 필드를 함께 정의한다.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum ProductErrorCode implements ErrorCode {

    PRODUCT_NOT_FOUND("PERR-4100", "상품을 찾을 수 없습니다"),
    ;

    private final String value;
    private final String message;
}
