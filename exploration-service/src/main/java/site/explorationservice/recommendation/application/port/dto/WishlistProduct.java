package site.explorationservice.recommendation.application.port.dto;

/**
 * product-service의 위시리스트 내부 API가 내려주는 상품 한 건. 지금은 productId만 씨앗으로 쓰지만,
 * genre·label·releaseYear·releaseCountry·pressType은 나중에 LLM 클러스터링을 붙일 때 재조회 없이 그대로 쓰려고
 * 처음부터 함께 받아둔다 — product-service 쪽에서도 같은 이유로 이 필드들을 API에 포함해뒀다.
 */
public record WishlistProduct(
    Long productId,
    String title,
    String artistName,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType
) {

}
