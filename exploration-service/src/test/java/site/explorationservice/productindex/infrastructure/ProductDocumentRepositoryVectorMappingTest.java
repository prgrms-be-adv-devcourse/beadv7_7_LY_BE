package site.explorationservice.productindex.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("색인 대상 벡터 매핑 확인")
class ProductDocumentRepositoryVectorMappingTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchIndicesClient indicesClient;

    @Mock
    private ProductVectorReader productVectorReader;

    private ProductDocumentRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ProductDocumentRepositoryImpl(
            elasticsearchOperations, elasticsearchClient, productVectorReader, "lp_products");
        given(elasticsearchClient.indices()).willReturn(indicesClient);
    }

    @Test
    @DisplayName("벡터 필드가 dense_vector 타입이면 준비된 것으로 본다")
    void 벡터_매핑이_있으면_참() throws IOException {
        // given
        given(indicesClient.getMapping(any(Function.class)))
            .willReturn(responseWith(record -> record.mappings(mapping -> mapping
                .properties("identityVector", property -> property.denseVector(vector -> vector)))));

        // when & then
        assertThat(repository.hasVectorMapping()).isTrue();
    }

    @Test
    @DisplayName("문서 저장이 자동 생성한 인덱스는 벡터가 숫자 배열이라 준비되지 않은 것으로 본다")
    void 자동_생성_매핑이면_거짓() throws IOException {
        // given
        // 매핑 없이 문서를 저장하면 ES가 float 배열을 보고 숫자 필드로 추측한다 — dense_vector가 될 수 없다
        given(indicesClient.getMapping(any(Function.class)))
            .willReturn(responseWith(record -> record.mappings(mapping -> mapping
                .properties("identityVector", property -> property.float_(number -> number)))));

        // when & then
        assertThat(repository.hasVectorMapping()).isFalse();
    }

    @Test
    @DisplayName("인덱스가 없어 조회가 404로 실패하면 준비되지 않은 것으로 본다")
    void 인덱스가_없으면_거짓() throws IOException {
        // given
        given(indicesClient.getMapping(any(Function.class)))
            .willThrow(new ElasticsearchException("indices.get_mapping", ErrorResponse.of(
                error -> error.status(404).error(cause -> cause
                    .type("index_not_found_exception").reason("no such index [lp_products]")))));

        // when & then
        assertThat(repository.hasVectorMapping()).isFalse();
    }

    @Test
    @DisplayName("매핑에 벡터 필드 자체가 없으면 준비되지 않은 것으로 본다")
    void 벡터_필드가_없으면_거짓() throws IOException {
        // given
        given(indicesClient.getMapping(any(Function.class)))
            .willReturn(responseWith(record -> record.mappings(mapping -> mapping
                .properties("title", property -> property.text(text -> text)))));

        // when & then
        assertThat(repository.hasVectorMapping()).isFalse();
    }

    /**
     * 별칭으로 조회해도 응답은 실제 인덱스 이름으로 키가 잡힌다 — 그 상황을 그대로 흉내 낸다.
     */
    private GetMappingResponse responseWith(
        final Function<IndexMappingRecord.Builder, IndexMappingRecord.Builder> record) {
        return GetMappingResponse.of(response -> response.mappings(
            Map.of("lp_products_v1", IndexMappingRecord.of(record::apply))));
    }
}
