package site.explorationservice.productindex.application.dto;

/**
 * 색인 결과.
 * <p>
 * embeddedText를 돌려주는 건 <b>템플릿을 비교하고 디버깅하기 위해서</b>다 — 어떤 문장이 이 벡터를 만들었는지,
 * 값 변환(pressType→한글, 연도→연대)이 의도대로 됐는지가 여기서 바로 드러난다. 문서에는 벡터만 남으므로
 * 색인 시점에 확인하지 않으면 나중에 되짚기 어렵다.
 * <p>
 * embeddingModel은 설정한 모델이 실제로 쓰였는지 확인하는 용도다 — Spring AI에 embedding.model과
 * embedding.options.model 두 경로가 모두 존재해서, 어느 쪽이 먹는지는 응답으로 확인하는 게 확실하다.
 */
public record ProductIndexResult(
    Long productId,
    String embeddedText,
    int dimensions,
    String embeddingModel,
    Integer totalTokens
) {

}
