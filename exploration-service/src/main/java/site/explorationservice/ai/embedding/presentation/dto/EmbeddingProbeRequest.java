package site.explorationservice.ai.embedding.presentation.dto;

import java.util.List;

/**
 * model·dimensions는 선택값이다. 비우면 application.yml의 설정을 그대로 쓰고,
 * 채우면 그 호출에만 적용된다 — 모델/차원 조합을 재기동 없이 비교하기 위한 것이다.
 */
public record EmbeddingProbeRequest(
    List<String> texts,
    String model,
    Integer dimensions
) {
}
