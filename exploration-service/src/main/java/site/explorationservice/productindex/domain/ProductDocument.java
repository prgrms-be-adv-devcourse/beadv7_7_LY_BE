package site.explorationservice.productindex.domain;

import java.util.List;
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
     * <p>
     * <b>surface 하위 필드는 검색의 아티스트 가산점 판정에만 쓴다.</b> 주 필드는 형태소 분석을 거쳐
     * 「장기하와 얼굴들」을 「장기하」로도 찾게 해주지만, 그 과정에서 「들국화」가 「들」·「국화」로도 쪼개진다.
     * 쪼개진 조각에 가산점이 붙으면 국화를 검색한 사람에게 들국화의 전 앨범이 최상위로 쏟아지므로,
     * 가산점은 표기가 통째로 남아 있는 이 필드에서만 판정한다.
     */
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "korean"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "surface", type = FieldType.Text, analyzer = "latin")
        }
    )
    private String artistName;

    /**
     * soft delete된 상품이 추천 결과에 나오는 걸 막기 위한 필터.
     */
    @Field(type = FieldType.Boolean)
    private Boolean active;

    /**
     * 다른 표기를 원문에 이어주는 값이다 — 「비틀즈」로 The Beatles 를, 「마일스 데이비스」로 Miles Davis 를 찾게 한다.
     * <p>
     * 주 필드의 분석기를 제목·아티스트명과 같게 맞추는 것이 중요하다. 검색 질의가 필드를 분석기별로 묶어
     * 처리하기 때문에, 별칭만 다른 분석기를 쓰면 「비틀즈 abbey road」처럼 별칭과 제목에 걸쳐 있는 검색어가
     * 어느 묶음도 만족시키지 못해 결과가 0건이 된다.
     * <p>
     * <b>아직 값이 비어 있다.</b> 별칭은 상품 테이블이 아니라 별도 테이블에 상품 1건당 여러 행으로 있어서
     * 공급원인 상품 서비스 내부 API가 조인해 내려줘야 한다.
     */
    @Field(type = FieldType.Text, analyzer = "korean")
    private List<String> titleAliases;

    /**
     * surface 하위 필드를 두는 이유는 artistName 과 같다 — 가산점 판정용이다.
     */
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "korean"),
        otherFields = @InnerField(suffix = "surface", type = FieldType.Text, analyzer = "latin")
    )
    private List<String> artistAliases;

    /**
     * 추천, 검색 api 응답에 들어가는 표시용 필드
     */
    @Field(type = FieldType.Keyword)
    private String coverImageUrl;

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
     * identity/origin/edition 그룹 상한(`ProductDocumentRepositoryImpl.capByGroup`)이 쓰는 그룹 키
     */
    @Field(type = FieldType.Keyword)
    private String identityGroupKey;

    @Field(type = FieldType.Keyword)
    private String originGroupKey;

    @Field(type = FieldType.Keyword)
    private String editionGroupKey;

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
