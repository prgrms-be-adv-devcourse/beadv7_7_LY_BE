package site.coreservice.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import site.coreservice.product.application.dto.ProductSearchResult;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.ProductSearchHit;
import site.coreservice.product.domain.ProductSearchPage;
import site.coreservice.product.domain.ProductSearchRepository;
import site.coreservice.product.exception.SearchKeywordRequiredException;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @InjectMocks
    private ProductSearchService productSearchService;

    private static final ProductSearchHit HIT =
            new ProductSearchHit(55L, "Abbey Road", "The Beatles", null, 1969, PressType.ORIGINAL, "UK");

    @Test
    @DisplayName("검색어가 없거나 공백이면 검색어 필수 예외를 던진다")
    void searchProducts_검색어_없으면_예외() {
        // given & when & then
        assertThatThrownBy(() -> productSearchService.searchProducts(null, 0, 20))
                .isInstanceOf(SearchKeywordRequiredException.class);
        assertThatThrownBy(() -> productSearchService.searchProducts("   ", 0, 20))
                .isInstanceOf(SearchKeywordRequiredException.class);
    }

    @Test
    @DisplayName("정규화하면 아무것도 남지 않는 검색어는 빈 페이지를 반환하고 저장소를 호출하지 않는다")
    void searchProducts_기호만이면_빈_페이지() {
        // given & when
        ProductSearchResult result = productSearchService.searchProducts("!!!", 0, 20);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.hasNext()).isFalse();
        then(productSearchRepository).should(never()).searchActiveByKeyword(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("검색어를 정규화해 저장소에 넘기고 결과를 조립한다")
    void searchProducts_정규화_후_조회() {
        // given
        given(productSearchRepository.searchActiveByKeyword("비틀즈", 0, 20))
                .willReturn(new ProductSearchPage(List.of(HIT), 1L));

        // when
        ProductSearchResult result = productSearchService.searchProducts("비틀즈!", 0, 20);

        // then
        assertThat(result.content()).containsExactly(HIT);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("size는 1 미만이면 기본값 20, 100 초과면 100으로 보정한다")
    void searchProducts_size_보정() {
        // given
        given(productSearchRepository.searchActiveByKeyword("비틀즈", 0, 20))
                .willReturn(new ProductSearchPage(List.of(), 0L));
        given(productSearchRepository.searchActiveByKeyword("비틀즈", 0, 100))
                .willReturn(new ProductSearchPage(List.of(), 0L));

        // when
        productSearchService.searchProducts("비틀즈", 0, 0);
        productSearchService.searchProducts("비틀즈", 0, 101);

        // then
        then(productSearchRepository).should().searchActiveByKeyword("비틀즈", 0, 20);
        then(productSearchRepository).should().searchActiveByKeyword("비틀즈", 0, 100);
    }

    @Test
    @DisplayName("page가 음수면 0으로 보정한다")
    void searchProducts_page_보정() {
        // given
        given(productSearchRepository.searchActiveByKeyword("비틀즈", 0, 20))
                .willReturn(new ProductSearchPage(List.of(), 0L));

        // when
        productSearchService.searchProducts("비틀즈", -3, 20);

        // then
        then(productSearchRepository).should().searchActiveByKeyword("비틀즈", 0, 20);
    }

    @Test
    @DisplayName("다음 페이지가 있으면 hasNext가 true다 (계산은 long으로 — 큰 page 값도 안전)")
    void searchProducts_hasNext_계산() {
        // given — 전체 3건, 페이지당 2건, 0페이지
        given(productSearchRepository.searchActiveByKeyword("비틀즈", 0, 2))
                .willReturn(new ProductSearchPage(List.of(HIT, HIT), 3L));

        // when
        ProductSearchResult result = productSearchService.searchProducts("비틀즈", 0, 2);

        // then
        assertThat(result.hasNext()).isTrue();
    }
}
