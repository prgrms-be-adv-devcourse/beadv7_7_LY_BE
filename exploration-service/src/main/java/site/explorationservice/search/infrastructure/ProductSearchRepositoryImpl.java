package site.explorationservice.search.infrastructure;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
 * 점수를 두 갈래로 나눠 매긴다.
 * <p>
 * <b>부스트 버킷</b>은 아티스트를 표기 그대로 지목했을 때만 발화해 100점을 얹는다. "아티스트에 걸린 상품은 제목에만
 * 걸린 상품보다 항상 위"라는 기존 규칙을 재현한다. 이게 없으면 유명 아티스트를 검색했을 때 그 이름이 제목에 들어간
 * 남의 앨범이 정작 본인 앨범보다 위로 올라온다.
 * <p>
 * <b>일반 버킷</b>은 형태소 분석을 거친 주 필드들을 본다. 조사를 떼기 때문에 「장기하와 얼굴들」이 「장기하」로도
 * 도달한다. 다만 여기엔 부스트를 주지 않는다 — 「들국화」가 「국화」로도 쪼개져 있어, 그 조각이 100점을 받으면
 * 국화를 검색한 사람의 결과가 들국화 앨범으로 도배된다.
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
            hits.getTotalHits());
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
        return Query.of(q -> q.bool(b -> b
            // filter가 있으면 should의 기본 최소 매칭 수가 0으로 떨어진다. 명시하지 않으면
            // 검색어에 하나도 안 걸린 문서까지 전부 결과에 들어온다.
            .minimumShouldMatch("1")
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
            .filter(f -> f.term(t -> t.field("active").value(true)))));
    }
}
