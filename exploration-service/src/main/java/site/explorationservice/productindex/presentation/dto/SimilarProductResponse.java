package site.explorationservice.productindex.presentation.dto;

/**
 * 기준 상품과 가까운 상품 하나.
 * <p>
 * score는 코사인 유사도를 0~1로 옮긴 값이라 <b>절대값보다 순서와 간격이 중요하다</b> — 임베딩 벡터끼리는 대체로 높게 나오므로 0.9라는 숫자 자체가 "많이
 * 닮았다"를 뜻하지 않는다. 1위와 5위의 점수 차가 붙어 있으면 그 템플릿은 상품을 잘 구분하지 못하고 있다는 신호다.
 */
public record SimilarProductResponse(
    Long productId,
    String title,
    String artistName,
    float score
) {

}
