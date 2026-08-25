package site.explorationservice.productindex.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import site.explorationservice.productindex.domain.ProductDocument;

@ExtendWith(MockitoExtension.class)
@DisplayName("색인 대상 인덱스")
class ProductDocumentRepositoryWriteTargetTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ProductVectorReader productVectorReader;

    @Captor
    private ArgumentCaptor<IndexCoordinates> targetCaptor;

    @Test
    @DisplayName("따로 지정하지 않으면 별칭에 색인한다")
    void 기본_대상은_별칭() {
        // given
        final ProductDocumentRepositoryImpl repository = new ProductDocumentRepositoryImpl(
            elasticsearchOperations, productVectorReader, "lp_products");

        // when
        repository.saveAll(List.of(ProductDocument.builder().productId(1L).build()));

        // then
        then(elasticsearchOperations).should().save(any(Iterable.class), targetCaptor.capture());
        assertThat(targetCaptor.getValue().getIndexName()).isEqualTo("lp_products");
    }

    @Test
    @DisplayName("대상을 지정하면 별칭이 아니라 그 인덱스에 색인한다")
    void 지정한_인덱스에_색인() {
        // given
        // 재색인 중에는 별칭이 옛 인덱스를 가리킨 채로 검색을 서비스하고 있어서,
        // 새 인덱스를 채우려면 별칭을 우회해 직접 써야 한다.
        final ProductDocumentRepositoryImpl repository = new ProductDocumentRepositoryImpl(
            elasticsearchOperations, productVectorReader, "lp_products_v2");

        // when
        repository.saveAll(List.of(ProductDocument.builder().productId(1L).build()));

        // then
        then(elasticsearchOperations).should().save(any(Iterable.class), targetCaptor.capture());
        assertThat(targetCaptor.getValue().getIndexName()).isEqualTo("lp_products_v2");
    }
}
