package site.coreservice.product.domain;

/** 검색 결과 1건 — 화면 카드에 필요한 값만 담는다. JPA든 검색엔진이든 어떤 구현체도 만들 수 있는 순수 데이터. */
public record ProductSearchHit(Long productId, String title, String artistName, String coverImageUrl,
        int releaseYear, PressType pressType) {
}
