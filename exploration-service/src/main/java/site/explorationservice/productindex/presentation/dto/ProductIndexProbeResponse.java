package site.explorationservice.productindex.presentation.dto;

import site.explorationservice.productindex.application.dto.ProductIndexResult;

/**
 * identity·origin·editionText가 이 응답의 핵심이다 — <b>문서에는 벡터만 남고 어떤 문장이 그 벡터를 만들었는지는 저장하지 않으므로</b>,
 * 색인 시점에 확인하지 않으면 나중에 Kibana로도 되짚을 수 없다. 값 변환이 의도대로 됐는지, 비어 있는 필드가 깔끔히 빠졌는지가 여기서 바로 드러난다.
 */
public record ProductIndexProbeResponse(
    Long productId,
    String identityText,
    String originText,
    String editionText,
    int dimensions,
    String embeddingModel,
    Integer totalTokens,
    long elapsedMs
) {

    public static ProductIndexProbeResponse from(final ProductIndexResult result,
        final long elapsedMs) {
        return new ProductIndexProbeResponse(
            result.productId(),
            result.identityText(),
            result.originText(),
            result.editionText(),
            result.dimensions(),
            result.embeddingModel(),
            result.totalTokens(),
            elapsedMs
        );
    }
}
