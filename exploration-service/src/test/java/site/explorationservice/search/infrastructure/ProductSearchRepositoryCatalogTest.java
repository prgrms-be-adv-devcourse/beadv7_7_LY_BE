package site.explorationservice.search.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.search.domain.SearchKeyword;

/**
 * 번호 조회가 검색 엔진에 실제로 무엇을 넘기는지 고정한다.
 * <p>
 * 질의 조립만 보는 테스트는 이 메서드가 어떤 값을 넣어 부르는지까지는 못 본다. 원문을 그대로 넘기거나 정렬을
 * 빼도 그쪽은 전부 통과하지만, 운영에서는 하이픈이 든 번호가 0건이 되고 페이지 순서가 흔들린다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("번호 조회 요청")
class ProductSearchRepositoryCatalogTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private ProductSearchRepositoryImpl productSearchRepository;

    @Test
    @DisplayName("원문이 아니라 표기를 통일한 값으로 대조한다")
    void 정규화_값으로_대조() {
        // given
        givenEmptyHits();

        // when
        productSearchRepository.searchByCatalogNumber(SearchKeyword.from("BLP-1567"), 0, 20);

        // then
        // 대조 대상 필드는 분석기를 타지 않는다. 하이픈이 든 원문을 그대로 넘기면 저장된 값과 안 맞아 0건이 된다
        final BoolQuery bool = captureQuery().getQuery().bool();
        assertThat(bool.should().get(0).term().value().stringValue()).isEqualTo("blp1567");
        assertThat(bool.should().get(1).prefix().value()).isEqualTo("blp1567");
    }

    @Test
    @DisplayName("점수와 상품 번호를 순서대로 정렬 키로 넘긴다")
    void 정렬_키_전달() {
        // given
        givenEmptyHits();

        // when
        productSearchRepository.searchByCatalogNumber(SearchKeyword.from("BLP-1567"), 0, 20);

        // then
        // 정렬을 안 넘기면 앞부분 일치로 점수가 같은 문서들의 순서가 요청마다 달라져,
        // 페이지를 넘기는 사이 같은 상품이 두 번 보이거나 빠진다
        final Sort sort = captureQuery().getSort();
        assertThat(sort).isNotNull();
        assertThat(sort).containsExactly(
                Sort.Order.desc("_score"),
                Sort.Order.asc("productId"));
    }

    private NativeQuery captureQuery() {
        final ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(ProductDocument.class));
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private void givenEmptyHits() {
        final SearchHits<ProductDocument> hits = mock(SearchHits.class);
        given(hits.getSearchHits()).willReturn(List.of());
        given(hits.getTotalHits()).willReturn(0L);
        given(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class))).willReturn(hits);
    }
}
