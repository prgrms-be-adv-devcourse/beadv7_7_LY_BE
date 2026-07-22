package site.coreservice.product.exception;

import site.common.exception.BusinessException;

/**
 * 상품이 존재하지 않을 때 (공개 조회에선 비활성 포함). common 핸들러가 404 + PERR-4100으로 변환한다.
 */
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException() {
        super(ProductErrorCode.PRODUCT_NOT_FOUND);
    }
}
