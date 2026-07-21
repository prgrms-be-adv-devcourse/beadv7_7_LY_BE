package site.coreservice.product.domain;

/**
 * 프레스 구분 (D-2). 같은 릴리스라도 프레스가 다르면 별개 상품으로 취급한다.
 * 세미는 ORIGINAL/REISSUE 2값. REMASTER는 파이널에서 추가.
 */
public enum PressType {
    ORIGINAL,
    REISSUE
}
