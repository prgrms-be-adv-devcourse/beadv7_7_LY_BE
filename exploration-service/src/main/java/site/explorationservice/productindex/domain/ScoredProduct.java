package site.explorationservice.productindex.domain;

/**
 * 유사도 검색으로 찾은 상품 하나와 그 점수.
 * <p>
 * score는 코사인 유사도를 0~1로 옮긴 값이라 <b>절대값보다 순서와 간격이 중요하다</b> — 임베딩 벡터끼리는 대체로 높게 나오므로 0.9라는 숫자 자체가 "많이
 * 닮았다"를 뜻하지 않는다.
 */
public record ScoredProduct(ProductDocument document, float score) {

}
