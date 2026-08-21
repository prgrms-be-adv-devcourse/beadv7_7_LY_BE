package site.explorationservice.productindex.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.KnnSimilarity;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * 검색·추천이 함께 보는 상품 read model. 원본은 MySQL(product-service 소유)이고 이건 파생 인덱스다.
 * <p>
 * <b>문서 id는 productId다.</b> ES는 같은 _id로 색인하면 덮어쓰므로, 같은 상품을 여러 번 색인해도 중복이
 * 생기지 않는다 — 나중에 상품 변경 이벤트를 구독할 때 at-least-once 전제의 멱등성이 별도 처리 없이 확보된다.
 * <p>
 * <b>지금은 추천에 필요한 최소 필드만 둔다.</b> 검색용 필드(label·발매정보·pressType·coverImage 등)는 검색을
 * 붙일 때 그쪽 요구에 맞춰 추가한다 — 필드를 나중에 <em>추가</em>하는 건 매핑 업데이트만으로 되고, 기존 문서에 값을 채워 넣더라도 <b>벡터는 그대로라 OpenAI
 * 재호출이 없다.</b> 비싼 부분이 반복되지 않으므로 미리 넣을 이유가 없다.
 * <p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "lp_products")
@Setting(settingPath = "elasticsearch/product-index-settings.json")
public class ProductDocument {

    @Id
    private Long productId;

    /**
     * 임베딩 텍스트에는 넣지 않는다 — 앨범 제목은 음악을 설명하지 않는 데다, 위시리스트 벡터를 평균낼 때 서로 무관한 제목들이 섞여 신호를 희석시킨다. 여기 두는 건
     * 추천 결과를 사람이 확인하기 위해서이고, 나중에 제목 검색이 붙으면 이 필드의 키워드 검색이 담당한다.
     */
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "korean"),
        otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String title;

    /**
     * 정확 일치 필터(artistName.keyword)와 검색(artistName) 양쪽에 쓰려고 멀티필드로 둔다.
     */
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "korean"),
        otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String artistName;

    /**
     * soft delete된 상품이 추천 결과에 나오는 걸 막기 위한 필터.
     */
    @Field(type = FieldType.Boolean)
    private Boolean active;

    /**
     * genre·label·releaseYear·releaseCountry·pressType — 검색·필터용이 아니라 <b>추천 결과를 사람이 종합적으로(아티스트명만이
     * 아니라) 판정하기 위한 표시용 필드</b>다. 임베딩 텍스트를 만드는 재료이기도 하지만 벡터로만 남기고 버리던 걸, identity/origin/edition 실측
     * 라운드가 반복되며 매번 MySQL을 수동으로 조인해야 했던 문제 때문에 저장하기로 했다(docs/recommendation-3vector-plan.md 1단계 참고).
     * 검색이 이 필드들을 실제로 쓰게 되면 그때 인덱싱 옵션(analyzer 등)을 재검토한다.
     */
    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Keyword)
    private String label;

    @Field(type = FieldType.Integer)
    private Integer releaseYear;

    @Field(type = FieldType.Keyword)
    private String releaseCountry;

    @Field(type = FieldType.Keyword)
    private String pressType;

    /**
     * 장르 + 아티스트("음악적 정체성").
     */
    @Field(
        type = FieldType.Dense_Vector,
        dims = 1024,
        knnSimilarity = KnnSimilarity.COSINE
    )
    private float[] identityVector;

    /**
     * 발매 연대 + 국가("시공간적 배경").
     */
    @Field(
        type = FieldType.Dense_Vector,
        dims = 1024,
        knnSimilarity = KnnSimilarity.COSINE
    )
    private float[] originVector;

    /**
     * 레이블 + 프레스타입("에디션 · 수집 가치").
     */
    @Field(
        type = FieldType.Dense_Vector,
        dims = 1024,
        knnSimilarity = KnnSimilarity.COSINE
    )
    private float[] editionVector;
}
