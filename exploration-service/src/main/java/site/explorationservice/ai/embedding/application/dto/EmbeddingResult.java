package site.explorationservice.ai.embedding.application.dto;

import java.util.List;

/**
 * 임베딩 호출 결과. 벡터만이 아니라 모델명·토큰 사용량까지 담는 건, 어떤 모델이 실제로 쓰였는지와 호출 비용을 호출자가 알 수 있어야 하기 때문이다 — 모델은 설정·요청
 * 양쪽에서 정해질 수 있고, 비용은 이 프로젝트에서 관리 대상으로 열어둔 항목이다.
 */
public record EmbeddingResult(
    List<float[]> vectors,
    String model,
    Integer promptTokens,
    Integer totalTokens
) {

    public float[] first() {
        return vectors.getFirst();
    }

    /**
     * 벡터 차원 수. dense_vector 매핑의 dims를 정하는 근거값이라 결과에서 바로 읽을 수 있게 둔다.
     */
    public int dimensions() {
        return vectors.isEmpty() ? 0 : vectors.getFirst().length;
    }
}
