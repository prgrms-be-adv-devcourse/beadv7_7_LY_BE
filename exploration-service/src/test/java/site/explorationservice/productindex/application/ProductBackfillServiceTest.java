package site.explorationservice.productindex.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.explorationservice.productindex.application.dto.BackfillResult;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;
import site.explorationservice.productindex.application.port.ProductPort;
import site.explorationservice.productindex.application.port.dto.ProductPage;
import site.explorationservice.productindex.domain.ProductDocumentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 백필")
class ProductBackfillServiceTest {

    @Mock
    private ProductPort productPort;

    @Mock
    private ProductIndexService productIndexService;

    @Mock
    private ProductDocumentRepository productDocumentRepository;

    @InjectMocks
    private ProductBackfillService productBackfillService;

    @Test
    @DisplayName("대상 인덱스의 벡터 매핑이 확인되지 않으면 상품을 읽기 전에 실패한다")
    void 매핑_미확인이면_시작_전에_실패() {
        // given
        // 인덱스가 없는 채로 저장하면 ES가 잘못된 타입으로 인덱스를 자동 생성하고도 에러를 내지 않으므로,
        // 시작 전에 막는 것이 유일한 방어선이다
        given(productDocumentRepository.hasVectorMapping()).willReturn(false);

        // when & then
        assertThatThrownBy(() -> productBackfillService.backfill(null, 100))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("색인 대상 인덱스");
        then(productPort).should(never()).findAllProducts(any(), anyInt());
        then(productIndexService).should(never()).indexAll(anyList());
    }

    @Test
    @DisplayName("매핑이 확인되면 페이지를 순회하며 색인한다")
    void 매핑_확인되면_색인_진행() {
        // given
        given(productDocumentRepository.hasVectorMapping()).willReturn(true);
        final List<ProductIndexCommand> items = List.of(command(1L), command(2L));
        given(productPort.findAllProducts(null, 100))
            .willReturn(new ProductPage(items, null, false));

        // when
        final BackfillResult result = productBackfillService.backfill(null, 100);

        // then
        assertThat(result.totalIndexed()).isEqualTo(2);
        assertThat(result.nextCursor()).isNull();
        assertThat(result.failedProductIds()).isEmpty();
        then(productIndexService).should().indexAll(items);
    }

    @Test
    @DisplayName("한 페이지가 실패해도 멈추지 않고 실패한 상품 번호를 모아 돌려준다")
    void 페이지_실패해도_계속_진행() {
        // given
        given(productDocumentRepository.hasVectorMapping()).willReturn(true);
        final List<ProductIndexCommand> firstPage = List.of(command(1L), command(2L));
        final List<ProductIndexCommand> secondPage = List.of(command(3L));
        given(productPort.findAllProducts(null, 100))
            .willReturn(new ProductPage(firstPage, 2L, true));
        given(productPort.findAllProducts(2L, 100))
            .willReturn(new ProductPage(secondPage, null, false));
        given(productIndexService.indexAll(firstPage))
            .willThrow(new RuntimeException("임베딩 호출 실패"));

        // when
        final BackfillResult result = productBackfillService.backfill(null, 100);

        // then
        assertThat(result.totalIndexed()).isEqualTo(1);
        assertThat(result.failedProductIds()).containsExactly(1L, 2L);
        assertThat(result.nextCursor()).isNull();
    }

    private ProductIndexCommand command(final Long productId) {
        return new ProductIndexCommand(productId, "title", "artist", null, null, null,
            null, null, null, true, null, null, List.of(), List.of());
    }
}
