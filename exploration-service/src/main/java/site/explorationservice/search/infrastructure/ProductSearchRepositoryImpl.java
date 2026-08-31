package site.explorationservice.search.infrastructure;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.search.domain.ProductSearchHit;
import site.explorationservice.search.domain.ProductSearchPage;
import site.explorationservice.search.domain.ProductSearchRepository;
import site.explorationservice.search.domain.SearchKeyword;

/**
 * 점수를 세 갈래로 나눠 매긴다.
 * <p>
 * <b>부스트 버킷</b>은 아티스트를 표기 그대로 지목했을 때만 발화해 100점을 얹는다. "아티스트에 걸린 상품은 제목에만
 * 걸린 상품보다 항상 위"라는 기존 규칙을 재현한다. 이게 없으면 유명 아티스트를 검색했을 때 그 이름이 제목에 들어간
 * 남의 앨범이 정작 본인 앨범보다 위로 올라온다.
 * <p>
 * <b>일반 버킷</b>은 형태소 분석을 거친 주 필드들을 본다. 조사를 떼기 때문에 「장기하와 얼굴들」이 「장기하」로도
 * 도달한다. 다만 여기엔 부스트를 주지 않는다 — 「들국화」가 「국화」로도 쪼개져 있어, 그 조각이 100점을 받으면
 * 국화를 검색한 사람의 결과가 들국화 앨범으로 도배된다.
 * <p>
 * <b>오타 버킷</b>은 같은 필드를 보되 단어마다 한 글자 차이를 같은 단어로 취급한다. 정확히 맞는 문서는 일반 버킷과
 * 이 버킷에 모두 걸려 점수가 합산되므로, 오타로 걸린 문서는 정확히 맞은 문서 아래에 놓인다. 단어를 쪼개 절로 나누는
 * 이유는 일반 버킷이 쓰는 교차 필드 매칭을 재현하기 위해서다 — 한 절로 두면 한 필드 안에 모든 단어가 있어야 해서,
 * 아티스트와 제목에 단어가 갈리는 검색어를 놓친다.
 */
@Repository
@RequiredArgsConstructor
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

    private static final float ARTIST_MATCH_BOOST = 100.0f;
    /**
     * 가산점 판정은 표기가 통째로 남아 있는 하위 필드에서만 한다. 주 필드는 형태소 분석을 거쳐 「들국화」가
     * 「국화」로도 쪼개져 있으므로, 주 필드로 판정하면 국화를 검색한 사람에게 들국화의 전 앨범이 최상위로 쏟아진다.
     */
    private static final List<String> ARTIST_SURFACE_FIELDS =
        List.of("artistName.surface", "artistAliases.surface");

    /**
     * <b>네 필드가 모두 같은 분석기를 쓴다는 것이 이 목록의 전제다.</b> 검색 엔진은 필드를 분석기별로 묶어
     * "모든 검색어가 한 묶음 안에 있어야 한다"를 묶음마다 따로 적용한다. 그래서 분석기가 섞이면
     * 「비틀즈 abbey road」처럼 검색어가 별칭과 제목에 걸쳐 있는 경우 어느 묶음도 만족하지 못해 결과가 0건이 된다.
     */
    private static final List<String> RELEVANCE_FIELDS = List.of(
        "title^3", "titleAliases^3", "artistName^1.5", "artistAliases^1.5");

    /**
     * 오타 절의 가중치. 정확히 맞는 문서는 이 절과 정확 매칭 절 양쪽에 걸려 점수가 합산되므로,
     * 이 값이 1을 넘지 않는 한 오타 결과가 정확한 결과를 밀어내지 못한다.
     */
    private static final float TYPO_BOOST = 0.5f;

    /**
     * 세 글자 이하는 정확히 맞아야 하고, 네 글자부터 한 글자 차이를 같은 단어로 본다.
     * 뒤의 99는 두 글자까지 허용하는 구간을 쓰지 않겠다는 뜻이다 — 그만큼 긴 단어가 없어 발화하지 않는다.
     * 짧은 단어를 흔들면 후보가 수십 개로 번지고, 두 글자를 허용하면 성이 닮은 다른 아티스트가 섞인다.
     */
    private static final String TYPO_FUZZINESS = "AUTO:4,99";

    /** 첫 글자는 제대로 쳤다고 보고 후보를 좁힌다. */
    private static final int TYPO_PREFIX_LENGTH = 1;

    /** 글자나 숫자를 하나라도 가진 조각만 조건으로 삼는다. */
    private static final Pattern SEARCHABLE_TOKEN = Pattern.compile(".*[\\p{L}\\p{N}].*");

    /**
     * 앞부분만 친 검색어를 받아주는 절의 가중치. 이름을 끝까지 친 사람이 항상 위에 와야 하므로 정확 매칭보다
     * 훨씬 낮게 두고, 앞부분이 겹칠 뿐인 다른 아티스트가 오타 결과보다는 쓸모 있으므로 오타 절보다는 높게 둔다.
     */
    private static final float PREFIX_BOOST = 0.7f;

    /** 정확 일치는 두 절 모두에 걸려 이미 위로 오고, 이 가산점은 앞부분만 겹치는 번호와의 점수 차를 더 벌린다. */
    private static final float CATALOG_EXACT_BOOST = 2.0f;
    private static final String CATALOG_FIELD = "normalizedCatalogNumber";

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public ProductSearchPage search(final SearchKeyword keyword, final int page, final int size) {
        final NativeQuery query = NativeQuery.builder()
            .withQuery(buildQuery(keyword.getValue()))
            .withPageable(PageRequest.of(page, size))
            // 기본값은 10,000에서 세기를 멈춘다. 프론트가 이 값으로 마지막 페이지를 판단한다.
            .withTrackTotalHits(true)
            .build();

        final SearchHits<ProductDocument> hits =
            elasticsearchOperations.search(query, ProductDocument.class);

        return new ProductSearchPage(
            hits.getSearchHits().stream().map(hit -> toSearchHit(hit.getContent())).toList(),
            hits.getTotalHits(),
            hits.getExecutionDuration().toMillis());
    }

    @Override
    public ProductSearchPage searchByCatalogNumber(final SearchKeyword keyword, final int page, final int size) {
        final NativeQuery query = NativeQuery.builder()
            .withQuery(buildCatalogQuery(keyword.getNormalized()))
            .withPageable(PageRequest.of(page, size))
            .withSort(catalogSort())
            .withTrackTotalHits(true)
            .build();

        final SearchHits<ProductDocument> hits =
            elasticsearchOperations.search(query, ProductDocument.class);

        return new ProductSearchPage(
            hits.getSearchHits().stream().map(hit -> toSearchHit(hit.getContent())).toList(),
            hits.getTotalHits(),
            hits.getExecutionDuration().toMillis());
    }

    /**
     * 색인 문서를 아는 곳은 이 클래스 하나다. 위 계층은 검색 결과 레코드만 본다 — 그래야 상품 색인 쪽이 문서에
     * 필드를 더하거나 빼도 검색의 응용·표현 계층이 따라 흔들리지 않는다.
     */
    static ProductSearchHit toSearchHit(final ProductDocument document) {
        return new ProductSearchHit(
            document.getProductId(),
            document.getTitle(),
            document.getArtistName(),
            document.getCoverImageUrl(),
            document.getReleaseYear(),
            document.getPressType());
    }

    /**
     * 질의 조립만 따로 뽑아 둔다 — Elasticsearch 없이도 이 메서드가 만든 {@link Query} 객체를 직접 들여다보면 두
     * should 버킷, minimum_should_match, 필드 가중치 같은 질의 구조를 검증할 수 있다.
     */
    static Query buildQuery(final String keyword) {
        final List<String> tokens = listSearchableTokens(keyword);

        return Query.of(q -> q.bool(b -> {
            // filter가 있으면 should의 기본 최소 매칭 수가 0으로 떨어진다. 명시하지 않으면
            // 검색어에 하나도 안 걸린 문서까지 전부 결과에 들어온다.
            b.minimumShouldMatch("1")
                .should(s -> s.constantScore(cs -> cs
                    .boost(ARTIST_MATCH_BOOST)
                    .filter(f -> f.multiMatch(mm -> mm
                        .query(keyword)
                        .operator(Operator.And)
                        .fields(ARTIST_SURFACE_FIELDS)))))
                .should(s -> s.multiMatch(mm -> mm
                    .query(keyword)
                    .type(TextQueryType.CrossFields)
                    .operator(Operator.And)
                    .fields(RELEVANCE_FIELDS)))
                // 이름을 끝까지 치지 않아도 찾게 한다. 주 필드는 형태소로 쪼개져 있어 「르세」가 「르세라핌」에
                // 닿지 못하므로, 표기가 통째로 남은 surface 필드에서만 앞부분을 맞춰본다.
                .should(s -> s.multiMatch(mm -> mm
                    .query(keyword)
                    .type(TextQueryType.PhrasePrefix)
                    .boost(PREFIX_BOOST)
                    .fields(ARTIST_SURFACE_FIELDS)));

            // 조건이 하나도 없는 묶음은 그 자체로 모든 문서에 걸린다. 그래서 얹을 단어가 없으면 버킷을 만들지 않는다.
            if (!tokens.isEmpty()) {
                b.should(buildTypoQuery(tokens));
            }

            return b.filter(f -> f.term(t -> t.field("active").value(true)));
        }));
    }

    /**
     * 조건으로 삼을 단어만 고른다. 기호만 있는 조각을 남기면 다듬은 뒤 아무 단어도 안 나오는 절이 되어,
     * 나머지 단어가 다 맞아도 묶음 전체가 탈락한다.
     */
    private static List<String> listSearchableTokens(final String keyword) {
        return Arrays.stream(keyword.split(" "))
            .filter(token -> SEARCHABLE_TOKEN.matcher(token).matches())
            .toList();
    }

    /**
     * 오타 버킷. 공백으로 나눈 단어마다 절을 만들어 전부 만족해야 통과시킨다 — 한 절에 몰아넣고 operator를 and로 주면
     * "모든 단어가 한 필드 안에" 있어야 해서, 아티스트와 제목에 단어가 나뉘어 걸리는 검색어를 놓친다.
     * 단어별로는 어느 필드에서 걸려도 되고, 각 단어가 한 글자까지 틀려도 같은 단어로 본다.
     * <p>
     * 어떤 단어를 다듬은 결과가 비면 그 절은 "맞는 문서 없음"이 되어 이 버킷 전체가 걸리지 않는다. 조사만 친
     * 검색어(「에서」)가 그런 경우인데, 그때를 "조건 없음"으로 바꿔주면 반대로 모든 문서가 걸리므로 그대로 둔다.
     */
    private static Query buildTypoQuery(final List<String> tokens) {
        final BoolQuery.Builder typo = new BoolQuery.Builder();
        for (final String token : tokens) {
            typo.must(m -> m.multiMatch(mm -> mm
                .query(token)
                .type(TextQueryType.BestFields)
                // 한 단어도 분석기를 거치면 여러 조각이 된다(「르세라핌」→「르」·「세라핌」).
                // 이 조각들 사이에 조건이 없으면 「르」 하나만 걸린 상품까지 전부 결과에 들어온다.
                .operator(Operator.And)
                .fuzziness(TYPO_FUZZINESS)
                .prefixLength(TYPO_PREFIX_LENGTH)
                .fields(RELEVANCE_FIELDS)));
        }

        return Query.of(q -> q.bool(typo.boost(TYPO_BOOST).build()));
    }

    /**
     * 대조하는 필드가 분석기를 타지 않으므로, 색인할 때와 같은 규칙으로 다듬은 값이 들어온다고 전제한다.
     */
    static Query buildCatalogQuery(final String normalized) {
        return Query.of(q -> q.bool(b -> b
            .minimumShouldMatch("1")
            .should(s -> s.term(t -> t.field(CATALOG_FIELD)
                .value(normalized)
                .boost(CATALOG_EXACT_BOOST)))
            .should(s -> s.prefix(p -> p.field(CATALOG_FIELD)
                .value(normalized)))
            .filter(f -> f.term(t -> t.field("active").value(true)))));
    }

    /**
     * 앞부분 일치만 걸린 문서는 점수가 전부 같다. 그 순서를 검색 엔진에 맡기면 요청마다 달라져, 페이지를
     * 넘기는 사이 같은 상품이 두 번 보이거나 빠진다. 값이 안 변하는 필드를 2차 키로 준다.
     * <p>
     * 이 필드는 글자로 저장돼 있어 정렬도 글자 순서다 (100, 1000, 99 순). 순서를 고정하는 것이 목적이라
     * 그대로 두지만, 결과가 번호 순으로 안 보이는 이유가 이것이다.
     */
    static Sort catalogSort() {
        return Sort.by(Sort.Order.desc("_score"), Sort.Order.asc("productId"));
    }
}
