package site.explorationservice.productindex.presentation.dto;

import java.util.List;
import site.explorationservice.productindex.application.ProductEmbeddingTemplate;
import site.explorationservice.productindex.application.dto.ProductIndexResult;

/**
 * 예시 상품을 한 번에 색인한 결과.
 * <p>
 * dimensions·model·totalTokens를 상품마다 반복하지 않고 위로 뺀 건 <b>한 번의 임베딩 호출로 전부 처리하기 때문</b>이다 — 토큰 수도 호출 단위로
 * 나오지 상품별로 나뉘지 않는다. 대신 상품별로는 어떤 문장이 벡터가 됐는지(embeddedText)만 남긴다. 템플릿을 바꿔 다시 넣었을 때 무엇이 달라졌는지 여기서 바로
 * 비교할 수 있다.
 */
public record SampleIndexResponse(
    int indexedCount,
    ProductEmbeddingTemplate template,
    int dimensions,
    String embeddingModel,
    Integer totalTokens,
    long elapsedMs,
    List<IndexedText> products
) {

    public record IndexedText(Long productId, String embeddedText) {

    }

    public static SampleIndexResponse from(final List<ProductIndexResult> results,
        final ProductEmbeddingTemplate template, final long elapsedMs) {
        final ProductIndexResult first = results.getFirst();

        return new SampleIndexResponse(
            results.size(),
            template,
            first.dimensions(),
            first.embeddingModel(),
            first.totalTokens(),
            elapsedMs,
            results.stream()
                .map(result -> new IndexedText(result.productId(), result.embeddedText()))
                .toList()
        );
    }
}
