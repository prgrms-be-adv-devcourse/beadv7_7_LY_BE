package site.explorationservice.search.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ConstantScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import site.explorationservice.productindex.domain.ProductDocument;

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
    @DisplayName("점수 버킷을 부스트용과 일반 관련도용 두 개로 나눈다")
    void should_버킷_두_개() {
        // given
        // when
        final BoolQuery bool = ProductSearchRepositoryImpl.buildQuery("장기하").bool();

        // then
        // 버킷이 하나로 합쳐지면 아티스트 정확 매치와 일반 관련도가 같은 잣대로 채점돼 부스트가 무의미해진다
        assertThat(bool.should()).hasSize(2);
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
}
