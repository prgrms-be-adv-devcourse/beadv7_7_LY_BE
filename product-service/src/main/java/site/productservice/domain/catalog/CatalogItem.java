package site.productservice.domain.catalog;

import site.productservice.domain.PressType;

/** 카탈로그 목록 카드 1건. JPQL 생성자 표현식이 이 순서로 값을 채운다. */
public record CatalogItem(Long productId, String title, String artistName, String coverImageUrl,
        int releaseYear, PressType pressType, String country) {
}
