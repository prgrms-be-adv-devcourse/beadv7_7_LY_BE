package site.explorationservice.productindex.presentation.dto;

import site.explorationservice.productindex.domain.ProductDocument;

/**
 * 색인된 문서를 되읽어 보여준다.
 * <p>
 * <b>벡터 값 자체는 담지 않고 색인 여부만 알린다.</b> ES 9는 dense_vector를 _source에서 빼기 때문에 문서를
 * 조회해도 벡터가 딸려오지 않는다 — 저장이 안 된 것처럼 보이지만 실제로는 별도 구조에 들어 있다. 그래서 값을 꺼내는 대신 "그 필드에 값이 있는가"를 따로 확인해
 * vectorIndexed로 알려준다.
 * <p>
 * 1024개 숫자를 그대로 뿌려도 읽을 수 없으니, 확인 목적에는 이쪽이 실용적이다. 실제 값을 봐야 하면 Kibana에서 검색의 fields 파라미터를 쓰면 된다.
 */
public record IndexedProductResponse(
    Long productId,
    String title,
    String artistName,
    Boolean active,
    boolean vectorIndexed
) {

    public static IndexedProductResponse of(final ProductDocument document,
        final boolean vectorIndexed) {
        return new IndexedProductResponse(
            document.getProductId(),
            document.getTitle(),
            document.getArtistName(),
            document.getActive(),
            vectorIndexed
        );
    }
}
