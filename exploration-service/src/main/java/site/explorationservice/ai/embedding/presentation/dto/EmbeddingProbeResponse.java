package site.explorationservice.ai.embedding.presentation.dto;

import java.util.List;
import site.explorationservice.ai.embedding.application.dto.EmbeddingResult;

/**
 * 벡터 전체(기본 1536개)를 그대로 뿌리면 응답을 읽을 수가 없어서, 값은 앞부분만 미리보기로 잘라서 담는다. 확인 목적상 필요한 건 실제 값이 아니라 "차원 수가 몇이고
 * 얼마나 걸렸는가"다.
 */
public record EmbeddingProbeResponse(
    int dimensions,
    int vectorCount,
    String model,
    Integer promptTokens,
    Integer totalTokens,
    long elapsedMs,
    List<Float> preview
) {

    private static final int PREVIEW_SIZE = 5;

    public static EmbeddingProbeResponse from(final EmbeddingResult result, final long elapsedMs) {
        return new EmbeddingProbeResponse(
            result.dimensions(),
            result.vectors().size(),
            result.model(),
            result.promptTokens(),
            result.totalTokens(),
            elapsedMs,
            preview(result.first())
        );
    }

    private static List<Float> preview(final float[] vector) {
        final int size = Math.min(PREVIEW_SIZE, vector.length);
        final List<Float> preview = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            preview.add(vector[i]);
        }
        return preview;
    }
}
