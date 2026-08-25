package site.explorationservice.productindex.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import site.explorationservice.productindex.domain.ProductDocument;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 인덱스 초기화")
class ProductIndexInitializerTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private IndexOperations entityOperations;

    @Mock
    private IndexOperations firstIndexOperations;

    @Test
    @DisplayName("별칭이 이미 있으면 인덱스를 새로 만들지 않는다")
    void 별칭_있으면_생성_안_함() {
        // given
        // 다음 버전으로 넘어간 뒤 앱을 붙였을 때 빈 첫 버전이 다시 생기면, 쓰이지도 않는 인덱스가 조용히 쌓인다.
        given(elasticsearchOperations.indexOps(ProductDocument.class)).willReturn(entityOperations);
        given(entityOperations.getAliases("lp_products"))
            .willReturn(Map.of("lp_products_v2", Set.<AliasData>of()));

        // when
        new ProductIndexInitializer(elasticsearchOperations).run(null);

        // then
        then(elasticsearchOperations).should(never()).indexOps(any(IndexCoordinates.class));
    }

    @Test
    @DisplayName("별칭이 없으면 첫 버전 인덱스를 만들고 별칭을 붙인다")
    void 별칭_없으면_생성() {
        // given
        given(elasticsearchOperations.indexOps(ProductDocument.class)).willReturn(entityOperations);
        given(entityOperations.getAliases("lp_products")).willReturn(Map.of());
        given(elasticsearchOperations.indexOps(IndexCoordinates.of("lp_products_v1")))
            .willReturn(firstIndexOperations);

        // when
        new ProductIndexInitializer(elasticsearchOperations).run(null);

        // then
        then(firstIndexOperations).should().create(any(), any());
        then(firstIndexOperations).should().alias(any());
    }
}
