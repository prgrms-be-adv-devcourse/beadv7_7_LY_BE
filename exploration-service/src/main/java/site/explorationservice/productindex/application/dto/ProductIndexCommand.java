package site.explorationservice.productindex.application.dto;

/**
 * 상품 하나를 색인하는 데 필요한 입력.
 * <p>
 * 여기 있는 값이 전부 ProductDocument의 필드가 되지는 않는다 — genre·label·발매정보·pressType은 <b>임베딩 텍스트를 만드는 재료</b>일
 * 뿐이고, 문서에는 벡터로만 남는다. 근거는 docs/search-recommendation-design-notes.md 참고.
 * <p>
 * 지금은 수동 트리거가 이 값을 그대로 채워 보내고, 나중에는 상품 변경 이벤트를 받은 리스너가 채운다.
 */
public record ProductIndexCommand(
    Long productId,
    String title,
    String artistName,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType,
    Boolean active
) {

}
