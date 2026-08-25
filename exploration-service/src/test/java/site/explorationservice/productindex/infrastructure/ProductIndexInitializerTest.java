package site.explorationservice.productindex.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import site.explorationservice.productindex.domain.ProductDocument;

/**
 * 기동할 때 인덱스를 어떻게 준비하는지 가른다.
 * <p>
 * 별칭 조회는 <b>없으면 빈 값이 아니라 예외를 던진다.</b> 이 클래스는 애플리케이션이 뜰 때 실행되므로 그 예외를
 * 그대로 두면 앱이 아예 시작되지 않는다. 아래 테스트들이 그 전제를 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("상품 인덱스 초기화")
class ProductIndexInitializerTest {

    private static final String ALIAS = "lp_products";
    private static final String FIRST_INDEX = "lp_products_v1";

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private IndexOperations entityOperations;

    @Mock
    private IndexOperations aliasNameOperations;

    @Mock
    private IndexOperations firstIndexOperations;

    @Test
    @DisplayName("별칭이 이미 있으면 인덱스를 새로 만들지 않는다")
    void 별칭_있으면_생성_안_함() {
        // given
        // 다음 버전으로 넘어간 뒤 앱을 붙였을 때 빈 첫 버전이 다시 생기면, 쓰이지도 않는 인덱스가 조용히 쌓인다.
        given(elasticsearchOperations.indexOps(ProductDocument.class)).willReturn(entityOperations);
        given(entityOperations.getAliases(ALIAS))
            .willReturn(Map.of("lp_products_v2", Set.<AliasData>of()));

        // when
        new ProductIndexInitializer(elasticsearchOperations).run(null);

        // then
        then(elasticsearchOperations).should(never()).indexOps(any(IndexCoordinates.class));
        then(entityOperations).should().putMapping();
    }

    @Test
    @DisplayName("별칭이 없다는 응답이 예외로 와도 기동을 멈추지 않고 첫 버전을 만든다")
    void 별칭_없으면_생성() {
        // given
        // 별칭 조회는 대상이 없으면 예외를 던진다. 이걸 그대로 두면 앱이 안 뜬다.
        given(elasticsearchOperations.indexOps(ProductDocument.class)).willReturn(entityOperations);
        willThrow(new ResourceNotFoundException("alias [" + ALIAS + "] missing"))
            .given(entityOperations).getAliases(ALIAS);
        given(elasticsearchOperations.indexOps(IndexCoordinates.of(ALIAS)))
            .willReturn(aliasNameOperations);
        given(aliasNameOperations.exists()).willReturn(false);
        given(elasticsearchOperations.indexOps(IndexCoordinates.of(FIRST_INDEX)))
            .willReturn(firstIndexOperations);
        given(firstIndexOperations.exists()).willReturn(false);

        // when
        new ProductIndexInitializer(elasticsearchOperations).run(null);

        // then
        then(firstIndexOperations).should().create(any(), any());
        then(firstIndexOperations).should().alias(any());
    }

    @Test
    @DisplayName("같은 이름을 실제 인덱스가 쓰고 있으면 만들지 않고 매핑만 갱신한다")
    void 같은_이름의_인덱스가_있으면() {
        // given
        // 별칭과 인덱스는 이름을 공유할 수 없다. 사람이 옛 인덱스를 지워야 풀리는 상황이라
        // 여기서 할 수 있는 일이 없고, 기동을 막으면 그 상태로 아무것도 못 하게 된다.
        given(elasticsearchOperations.indexOps(ProductDocument.class)).willReturn(entityOperations);
        willThrow(new ResourceNotFoundException("alias [" + ALIAS + "] missing"))
            .given(entityOperations).getAliases(ALIAS);
        given(elasticsearchOperations.indexOps(IndexCoordinates.of(ALIAS)))
            .willReturn(aliasNameOperations);
        given(aliasNameOperations.exists()).willReturn(true);

        // when
        new ProductIndexInitializer(elasticsearchOperations).run(null);

        // then
        then(elasticsearchOperations).should(never())
            .indexOps(IndexCoordinates.of(FIRST_INDEX));
        then(entityOperations).should().putMapping();
    }

    @Test
    @DisplayName("첫 버전 인덱스만 남아 있으면 다시 만들지 않고 별칭만 붙인다")
    void 인덱스만_있고_별칭이_없으면() {
        // given
        // 인덱스 생성과 별칭 부착은 별개의 요청이라 앞만 성공한 채 끝날 수 있다.
        // 그때 다시 만들려 들면 이름이 겹쳐 실패하고, 사람이 지우기 전까지 계속 못 뜬다.
        given(elasticsearchOperations.indexOps(ProductDocument.class)).willReturn(entityOperations);
        willThrow(new ResourceNotFoundException("alias [" + ALIAS + "] missing"))
            .given(entityOperations).getAliases(ALIAS);
        given(elasticsearchOperations.indexOps(IndexCoordinates.of(ALIAS)))
            .willReturn(aliasNameOperations);
        given(aliasNameOperations.exists()).willReturn(false);
        given(elasticsearchOperations.indexOps(IndexCoordinates.of(FIRST_INDEX)))
            .willReturn(firstIndexOperations);
        given(firstIndexOperations.exists()).willReturn(true);

        // when
        new ProductIndexInitializer(elasticsearchOperations).run(null);

        // then
        then(firstIndexOperations).should(never()).create(any(), any());
        then(firstIndexOperations).should().alias(any());
    }
}
