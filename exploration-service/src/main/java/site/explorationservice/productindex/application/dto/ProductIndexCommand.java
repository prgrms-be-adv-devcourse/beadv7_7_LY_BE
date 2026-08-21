package site.explorationservice.productindex.application.dto;

/**
 * 상품 하나를 색인하는 데 필요한 입력.
 * <p>
 * genre·label·releaseYear·releaseCountry·pressType은 identity·origin·edition 임베딩 텍스트를 만드는 재료이자, 동시에
 * {@code ProductDocument}에 표시용으로 그대로 저장된다(추천 결과를 사람이 아티스트명뿐 아니라 종합적으로 판단하기 위해서 —
 * docs/recommendation-3vector-plan.md 1단계 참고). coverImageUrl은 표시 전용이라 임베딩 텍스트에는 들어가지 않는다.
 * <p>
 * 지금은 수동 트리거(백필)가 이 값을 그대로 채워 보내고, 나중에는 상품 변경 이벤트를 받은 리스너가 채운다.
 */
public record ProductIndexCommand(
    Long productId,
    String title,
    String artistName,
    String coverImageUrl,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType,
    Boolean active
) {

}
