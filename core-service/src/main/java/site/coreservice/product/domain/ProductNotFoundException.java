package site.coreservice.product.domain;

import lombok.Getter;
import site.common.exception.ErrorCode;

/**
 * 상품이 존재하지 않을 때 (공개 조회에선 비활성 포함). 404 + PERR-4100으로 변환된다.
 * TODO (#48) : common BusinessException merge 후 상속 대상을 BusinessException으로 교체한다.
 */
@Getter
public class ProductNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;

    public ProductNotFoundException() {
        super(ProductErrorCode.PRODUCT_NOT_FOUND.getMessage());
        this.errorCode = ProductErrorCode.PRODUCT_NOT_FOUND;
    }
}
