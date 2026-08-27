package site.explorationservice.search.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ConstantScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.search.domain.ProductSearchHit;

/**
 * {@link ProductSearchRepositoryImpl#buildQuery(String)}가 조립한 질의의 구조를 검증한다.
 * <p>
 * 실제 검색 결과 순위(런타임 동작)는 Elasticsearch가 있어야 확인할 수 있지만, 이 다섯 가지는 질의 "구조"라
 * 네트워크 없이도 {@link Query} 객체를 직접 들여다보고 고정할 수 있다. 누가 상수를 실수로 건드리면 이 테스트가
 * 즉시 잡아낸다 — 수동 스모크 테스트까지 기다릴 필요가 없다.
 */
@DisplayName("상품 검색 질의 구조")
class ProductSearchQueryTest {

    @Test
    @DisplayName("점수 버킷을 부스트용·일반 관련도용·오타용 세 개로 나눈다")
    void should_버킷_세_개() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("장기하").bool();

        // then
        // 버킷이 하나로 합쳐지면 아티스트 정확 매치와 일반 관련도가 같은 잣대로 채점돼 부스트가 무의미해진다
        assertThat(bool.should()).hasSize(3);
    }

    @Test
    @DisplayName("minimum_should_match를 1로 명시한다")
    void minimum_should_match_1() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("장기하").bool();

        // then
        // filter가 있으면 bool 질의의 minimum_should_match 기본값이 0으로 떨어져, should에 하나도
        // 안 걸린 문서(즉 검색어와 무관한 모든 활성 상품)까지 결과에 섞여 들어온다
        assertThat(bool.minimumShouldMatch()).isEqualTo("1");
    }

    @Test
    @DisplayName("부스트 버킷은 형태소 분해 필드 없이 표기 그대로인 필드만 본다")
    void 부스트_버킷_필드는_형태소_분해_제외() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("들국화").bool();
        final ConstantScoreQuery boostBucket = bool.should().get(0).constantScore();
        final MultiMatchQuery exactMatch = boostBucket.filter().multiMatch();

        // then
        // 주 필드는 조사를 떼며 형태소로 쪼갠다 — 「들국화」가 「들」·「국화」로 나뉘는데, 이 조각이
        // 부스트 버킷에 들어가면 100점을 받아 "국화"를 검색한 사람의 결과가 들국화 앨범으로 도배된다.
        // 그래서 표기가 통째로 남아 있는 surface 하위 필드에서만 판정한다
        assertThat(exactMatch.fields()).containsExactly("artistName.surface", "artistAliases.surface");
        assertThat(boostBucket.boost()).isEqualTo(100.0f);
    }

    @Test
    @DisplayName("일반 버킷은 cross_fields로 제목·아티스트 필드에 지정된 가중치를 준다")
    void 일반_버킷_가중치() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("장기하").bool();
        final MultiMatchQuery relevanceBucket = bool.should().get(1).multiMatch();

        // then
        // 가중치가 흐트러지면 "제목에만 스치듯 걸린 상품"과 "아티스트에 제대로 걸린 상품"의 우선순위가
        // 뒤바뀐다 — title^3 등은 설계에서 정한 값이라 리팩터링 중에 실수로 바뀌기 쉽다
        assertThat(relevanceBucket.type()).isEqualTo(TextQueryType.CrossFields);
        assertThat(relevanceBucket.fields()).containsExactly(
            "title^3", "titleAliases^3", "artistName^1.5", "artistAliases^1.5");
    }

    @Test
    @DisplayName("삭제된 상품을 걸러내는 active=true term filter를 건다")
    void active_필터() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("장기하").bool();
        final TermQuery activeFilter = bool.filter().get(0).term();

        // then
        // 이 필터가 빠지면 soft delete된 상품이 검색 결과에 다시 노출된다
        assertThat(activeFilter.field()).isEqualTo("active");
        assertThat(activeFilter.value().booleanValue()).isTrue();
    }

    @Test
    @DisplayName("일반 관련도 버킷의 필드가 전부 문서에 존재한다")
    void 일반_버킷_필드는_문서에_존재() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("장기하").bool();
        final MultiMatchQuery relevanceBucket = bool.should().get(1).multiMatch();

        // then
        // 없는 필드를 질의해도 검색엔진은 에러 없이 0건을 돌려준다 — 이름이 어긋나면 컴파일도 되고
        // 기존 문자열 비교 테스트도 통과한 채 검색 결과만 조용히 나빠지므로, 실제 문서 필드와 대조해야 잡힌다
        assertThat(relevanceBucket.fields())
            .allSatisfy(field -> assertThat(existsInDocument(stripBoost(field)))
                .as("필드 '%s'가 ProductDocument에 존재해야 한다", field)
                .isTrue());
    }

    @Test
    @DisplayName("부스트 버킷의 필드가 전부 문서에 존재한다")
    void 부스트_버킷_필드는_문서에_존재() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("들국화").bool();
        final ConstantScoreQuery boostBucket = bool.should().get(0).constantScore();
        final MultiMatchQuery exactMatch = boostBucket.filter().multiMatch();

        // then
        // 없는 필드를 질의해도 검색엔진은 에러 없이 0건을 돌려준다 — 이름이 어긋나면 컴파일도 되고
        // 기존 문자열 비교 테스트도 통과한 채 검색 결과만 조용히 나빠지므로, 실제 문서 필드와 대조해야 잡힌다
        assertThat(exactMatch.fields())
            .allSatisfy(field -> assertThat(existsInDocument(stripBoost(field)))
                .as("필드 '%s'가 ProductDocument에 존재해야 한다", field)
                .isTrue());
    }

    /** 오타 절만 골라낸다 — should 절의 순서에 기대지 않으려고 유일한 bool 절이라는 점으로 찾는다. */
    private BoolQuery typoClause(final String keyword) {
        return ProductSearchRepositoryImpl.buildQuery(keyword).bool().should().stream()
            .filter(Query::isBool)
            .map(Query::bool)
            .findFirst()
            .orElseThrow(() -> new AssertionError("오타를 허용하는 절이 없다"));
    }

    @Test
    @DisplayName("오타 절은 검색어의 단어마다 절을 만들어 전부 만족하도록 묶는다")
    void 오타_절은_단어마다_한_절() {
        // given
        // when
        final BoolQuery typo = typoClause("Abigail Washbburn City Of Refuge");

        // then
        // 한 절에 몰아넣고 operator를 and로 주면 "모든 단어가 한 필드 안에" 있어야 해서,
        // 아티스트와 제목에 단어가 갈리는 검색어를 통째로 놓친다
        assertThat(typo.must()).hasSize(5);
        assertThat(typo.should()).isEmpty();
    }

    @Test
    @DisplayName("오타 절의 각 단어는 세 글자 이하를 정확 매칭으로 두고 첫 글자를 고정한다")
    void 오타_절_임계값() {
        // given
        // when
        final BoolQuery typo = typoClause("Abigail Washbburn");

        // then
        // 3글자 이하까지 흔들면 are·man·100 같은 단어가 수십 개 이웃으로 번진다
        assertThat(typo.must()).allSatisfy(clause -> {
            // cross_fields로 바뀌면 오타 허용과 같이 쓸 수 없어 검색 요청이 전부 실패한다
            assertThat(clause.multiMatch().type()).isEqualTo(TextQueryType.BestFields);
            assertThat(clause.multiMatch().fuzziness()).isEqualTo("AUTO:4,99");
            assertThat(clause.multiMatch().prefixLength()).isEqualTo(1);
            assertThat(clause.multiMatch().fields()).containsExactly(
                "title^3", "titleAliases^3", "artistName^1.5", "artistAliases^1.5");
        });
    }

    @Test
    @DisplayName("오타 절은 기호가 섞인 검색어에서 기호를 뺀 단어만 조건으로 삼는다")
    void 오타_절은_기호를_조건에서_뺀다() {
        // given
        // when
        final BoolQuery typo = typoClause("Yes Fly From Here - Retrn Trip");

        // then
        // 기호를 조건으로 남기면 다듬은 뒤 남는 단어가 없어 "맞는 문서 없음"이 되고,
        // 모두 만족해야 하는 묶음이라 나머지 단어가 다 맞아도 결과가 0건이 된다
        assertThat(typo.must()).hasSize(6);
    }

    @Test
    @DisplayName("오타 절은 단어가 사라진 절을 조건 없음으로 바꾸지 않는다")
    void 오타_절은_빈_절을_통과시키지_않는다() {
        // given
        // when
        final BoolQuery typo = typoClause("장기하 에서");

        // then
        // 조사만 친 검색어는 다듬고 나면 남는 단어가 없다. 이걸 "조건 없음"으로 두면 모든 절이 그렇게 되는
        // 검색어에서 카탈로그 43.5만 건이 통째로 결과가 된다. 걸리지 않는 쪽이 안전하다
        assertThat(typo.must()).allSatisfy(clause ->
            assertThat(clause.multiMatch().zeroTermsQuery()).isNull());
    }

    @Test
    @DisplayName("기호뿐인 검색어에는 오타 버킷을 얹지 않는다")
    void 기호뿐인_검색어() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("- - -").bool();

        // then
        // 조건이 하나도 없는 묶음은 그 자체로 모든 문서에 걸려, 카탈로그 43.5만 건이 통째로 나간다
        assertThat(bool.should()).hasSize(2);
    }

    @Test
    @DisplayName("오타 절은 정확 매칭 절보다 낮은 가중치를 받는다")
    void 오타_절_가중치() {
        // given
        // when
        final BoolQuery typo = typoClause("coltrane");

        // then
        // 정확히 맞는 문서는 두 절에 모두 걸려 점수가 합산된다. 오타 절이 세면 그 우위가 뒤집히고,
        // 0이면 오타로 걸린 문서가 점수를 못 받아 결과 맨 뒤로 밀린다
        assertThat(typo.boost()).isBetween(0.1f, 0.9f);
    }

    @Test
    @DisplayName("기존 cross_fields 절에는 오타 허용을 걸지 않는다")
    void 기존_절은_그대로() {
        // given
        // when
        final MultiMatchQuery crossFields = ProductSearchRepositoryImpl.buildQuery("장기하").bool()
            .should().stream()
            .filter(Query::isMultiMatch)
            .map(Query::multiMatch)
            .filter(mm -> mm.type() == TextQueryType.CrossFields)
            .findFirst()
            .orElseThrow();

        // then
        // cross_fields는 여러 필드의 단어 통계를 하나로 합쳐 채점하는데, 오타 허용은 단어 하나를
        // 여러 후보로 늘려서 합칠 대상이 사라진다. 검색 엔진이 이 조합을 거부하므로 실수로 얹으면
        // 검색 요청이 전부 실패한다
        assertThat(crossFields.fuzziness()).isNull();
    }

    /**
     * 색인 문서에서 검색 결과 레코드로 옮기는 자리는 필드가 전부 문자열이라, 순서를 바꿔 넣어도 컴파일이 통과한다.
     * 아티스트명 자리에 표지 이미지 주소가 들어가는 식의 사고가 화면에 뜨기 전까지 드러나지 않으므로 값으로 고정한다.
     */
    @Test
    @DisplayName("색인 문서의 값을 검색 결과 레코드로 자리 바꿈 없이 옮긴다")
    void 문서를_검색결과로_변환() {
        // given
        final ProductDocument document = ProductDocument.builder()
            .productId(42L)
            .title("별일 없이 산다")
            .artistName("장기하와 얼굴들")
            .coverImageUrl("https://img.example.com/42.jpg")
            .releaseYear(2009)
            .pressType("ORIGINAL")
            .build();

        // when
        final ProductSearchHit hit = ProductSearchRepositoryImpl.toSearchHit(document);

        // then
        assertThat(hit.productId()).isEqualTo(42L);
        assertThat(hit.title()).isEqualTo("별일 없이 산다");
        assertThat(hit.artistName()).isEqualTo("장기하와 얼굴들");
        assertThat(hit.coverImageUrl()).isEqualTo("https://img.example.com/42.jpg");
        assertThat(hit.releaseYear()).isEqualTo(2009);
        assertThat(hit.pressType()).isEqualTo("ORIGINAL");
    }

    /**
     * 표시 필드는 상품 서비스 내부 API 확장과 재색인이 끝나야 채워진다. 그전까지 비어서 들어오는데,
     * 그때 0이나 빈 문자열을 지어내면 화면에 0년으로 표시된다.
     */
    @Test
    @DisplayName("비어 있는 표시 필드를 지어내지 않고 그대로 옮긴다")
    void 빈_표시필드_변환() {
        // given
        final ProductDocument document = ProductDocument.builder()
            .productId(7L)
            .title("Tutu")
            .artistName("Miles Davis")
            .build();

        // when
        final ProductSearchHit hit = ProductSearchRepositoryImpl.toSearchHit(document);

        // then
        assertThat(hit.coverImageUrl()).isNull();
        assertThat(hit.releaseYear()).isNull();
        assertThat(hit.pressType()).isNull();
    }

    /**
     * 가중치 표기("title^3")에서 필드명("title")만 떼어낸다.
     */
    private static String stripBoost(final String weightedFieldName) {
        final int caretIndex = weightedFieldName.indexOf('^');
        return caretIndex == -1 ? weightedFieldName : weightedFieldName.substring(0, caretIndex);
    }

    /**
     * 질의 필드명이 {@link ProductDocument}에 실제로 선언돼 있는지 리플렉션으로 확인한다.
     * <p>
     * "."이 없으면 자바 필드 이름(또는 {@code @Field(name=...)} 지정값)과 일치해야 하고, "."이 있으면
     * (예: "artistName.surface") 앞부분이 {@code @MultiField}가 걸린 필드여야 하고 뒷부분이 그 서브필드
     * ({@code @InnerField})의 suffix와 일치해야 한다.
     */
    private static boolean existsInDocument(final String qualifiedFieldName) {
        final String[] parts = qualifiedFieldName.split("\\.", 2);
        final String topLevelFieldName = parts[0];

        for (final java.lang.reflect.Field declaredField : ProductDocument.class.getDeclaredFields()) {
            if (!resolveEsFieldName(declaredField).equals(topLevelFieldName)) {
                continue;
            }
            if (parts.length == 1) {
                return true;
            }
            return hasInnerFieldSuffix(declaredField, parts[1]);
        }
        return false;
    }

    /**
     * ES 필드명을 결정한다. {@code @Field(name=...)}(또는 {@code @MultiField}의 mainField에 지정된
     * name)이 비어 있지 않으면 그 값을, 아니면 자바 필드 이름을 그대로 쓴다(Spring Data Elasticsearch의
     * 기본 동작).
     */
    private static String resolveEsFieldName(final java.lang.reflect.Field declaredField) {
        final Field fieldAnnotation = declaredField.getAnnotation(Field.class);
        if (fieldAnnotation != null && !fieldAnnotation.name().isEmpty()) {
            return fieldAnnotation.name();
        }
        final MultiField multiFieldAnnotation = declaredField.getAnnotation(MultiField.class);
        if (multiFieldAnnotation != null && !multiFieldAnnotation.mainField().name().isEmpty()) {
            return multiFieldAnnotation.mainField().name();
        }
        return declaredField.getName();
    }

    private static boolean hasInnerFieldSuffix(final java.lang.reflect.Field declaredField, final String suffix) {
        final MultiField multiFieldAnnotation = declaredField.getAnnotation(MultiField.class);
        if (multiFieldAnnotation == null) {
            return false;
        }
        for (final InnerField innerField : multiFieldAnnotation.otherFields()) {
            if (innerField.suffix().equals(suffix)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("번호 질의는 정확 일치와 앞부분 일치 두 절을 가진다")
    void 번호_질의_두_절() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildCatalogQuery("blp1567").bool();

        // then
        // 정확 일치만 두면 번호를 일부만 아는 사람이 못 찾고, 앞부분 일치만 두면 정확히 친 번호가 밀린다
        assertThat(bool.should()).hasSize(2);
        assertThat(bool.minimumShouldMatch()).isEqualTo("1");
    }

    @Test
    @DisplayName("정확 일치가 앞부분 일치보다 높은 가중치를 받는다")
    void 정확_일치_우선() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildCatalogQuery("blp1567").bool();
        final TermQuery exact = bool.should().get(0).term();
        final PrefixQuery prefix = bool.should().get(1).prefix();

        // then
        assertThat(exact.field()).isEqualTo("normalizedCatalogNumber");
        assertThat(prefix.field()).isEqualTo("normalizedCatalogNumber");
        // 앞부분 일치에는 가중치를 주지 않는다 — 두 절의 차이가 곧 정확 일치를 위로 올리는 힘이다
        assertThat(exact.boost()).isEqualTo(2.0f);
        assertThat(prefix.boost()).isNull();
    }

    @Test
    @DisplayName("번호 질의도 판매중인 상품만 본다")
    void 번호_질의_활성_필터() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildCatalogQuery("blp1567").bool();

        // then
        // 이게 빠지면 내린 상품이 번호로는 계속 검색된다
        assertThat(bool.filter()).hasSize(1);
        assertThat(bool.filter().get(0).term().field()).isEqualTo("active");
    }

    @Test
    @DisplayName("번호 질의는 점수 다음 정렬 키로 상품 번호를 준다")
    void 번호_질의_2차_정렬() {
        // given
        // when
        final Sort sort = ProductSearchRepositoryImpl.catalogSort();

        // then
        // 2차 키가 없으면 페이지를 넘기는 사이 순서가 흔들려 같은 상품이 두 번 보이거나 빠진다
        assertThat(sort).containsExactly(
                Sort.Order.desc("_score"),
                Sort.Order.asc("productId"));
    }
}
