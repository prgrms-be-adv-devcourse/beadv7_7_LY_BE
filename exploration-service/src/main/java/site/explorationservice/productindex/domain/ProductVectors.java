package site.explorationservice.productindex.domain;

/**
 * 상품 하나의 identity·origin·edition 3벡터. 씨앗 상품들의 벡터를 되읽을 때(추천의 평균 계산용)와, kNN 질의 벡터를 조립할 때 양쪽에 쓰인다.
 */
public record ProductVectors(
    float[] identityVector,
    float[] originVector,
    float[] editionVector
) {

}
