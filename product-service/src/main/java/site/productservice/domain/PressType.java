package site.productservice.domain;

/**
 * 몇 번째로 찍어낸 판인지 구분 (초판 ORIGINAL / 재발매 REISSUE).
 * 같은 앨범이라도 프레스가 다르면 별개 상품으로 취급한다. REMASTER는 파이널에서 추가.
 */
public enum PressType {
    ORIGINAL,
    REISSUE
}
