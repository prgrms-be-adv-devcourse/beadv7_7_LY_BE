package site.explorationservice.search.domain;

/**
 * 검색 결과 1건 — 화면 카드에 필요한 값만 담는다.
 * <p>
 * 색인 문서(ProductDocument)를 그대로 위로 올리지 않는 이유는 두 가지다. 그 문서는 상품 색인 쪽이 소유한 타입이라
 * 검색이 붙들고 있으면 두 기능이 서로의 사정에 묶이고, 벡터 같은 검색과 무관한 필드까지 바깥 계층에 딸려 나온다.
 * 여기서 걸러 두면 문서를 아는 곳이 검색의 저장소 구현 한 곳으로 좁혀진다.
 */
public record ProductSearchHit(Long productId, String title, String artistName, String coverImageUrl,
    Integer releaseYear, String pressType) {
}
