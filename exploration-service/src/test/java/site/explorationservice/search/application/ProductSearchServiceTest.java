package site.explorationservice.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.explorationservice.search.application.dto.ProductSearchResult;
import site.explorationservice.search.domain.ProductSearchHit;
import site.explorationservice.search.domain.ProductSearchPage;
import site.explorationservice.search.domain.ProductSearchRepository;
import site.explorationservice.search.domain.SearchKeyword;
import site.explorationservice.search.exception.SearchKeywordRequiredException;
import site.explorationservice.search.exception.UnsupportedSearchTargetException;
import site.explorationservice.searchlog.application.SearchLogService;
import site.explorationservice.searchlog.application.dto.SearchLogCommand;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 검색")
class ProductSearchServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private SearchLogService searchLogService;

    @InjectMocks
    private ProductSearchService productSearchService;

    @Test
    @DisplayName("검색어가 없으면 예외를 던진다")
    void 검색어_없으면_예외() {
        // given
        final String keyword = null;

        // when & then
        assertThatThrownBy(() -> productSearchService.searchProducts(keyword, null, 0, 20))
            .isInstanceOf(SearchKeywordRequiredException.class);
        verify(productSearchRepository, never()).search(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("검색어가 공백뿐이면 예외를 던진다")
    void 공백_검색어면_예외() {
        // given
        final String keyword = "   ";

        // when & then
        assertThatThrownBy(() -> productSearchService.searchProducts(keyword, null, 0, 20))
            .isInstanceOf(SearchKeywordRequiredException.class);
    }

    @Test
    @DisplayName("한 글자 검색어는 조회하지 않고 빈 결과를 돌려준다")
    void 짧은_검색어는_빈_결과() {
        // given
        final String keyword = "a";

        // when
        final ProductSearchResult result = productSearchService.searchProducts(keyword, null, 0, 20);

        // then
        // 400으로 던지면 프론트가 응답 형식을 두 갈래로 다뤄야 한다. 형식을 유지한 채 결과만 비운다.
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.hasNext()).isFalse();
        verify(productSearchRepository, never()).search(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("음수 페이지는 0으로 보정한다")
    void 음수_페이지_보정() {
        // given
        given(productSearchRepository.search(any(), eq(0), anyInt()))
            .willReturn(ProductSearchPage.empty());

        // when
        final ProductSearchResult result = productSearchService.searchProducts("장기하", null, -5, 20);

        // then
        assertThat(result.page()).isZero();
        verify(productSearchRepository).search(any(), eq(0), eq(20));
    }

    @Test
    @DisplayName("size가 0 이하이면 기본값 20으로 되돌린다")
    void size_기본값() {
        // given
        given(productSearchRepository.search(any(), anyInt(), eq(20)))
            .willReturn(ProductSearchPage.empty());

        // when
        final ProductSearchResult result = productSearchService.searchProducts("장기하", null, 0, 0);

        // then
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("size가 상한을 넘으면 100으로 깎는다")
    void size_상한() {
        // given
        given(productSearchRepository.search(any(), anyInt(), eq(100)))
            .willReturn(ProductSearchPage.empty());

        // when
        final ProductSearchResult result = productSearchService.searchProducts("장기하", null, 0, 5000);

        // then
        // 상한이 없으면 한 번의 요청으로 인덱스 전체를 긁어갈 수 있다
        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("다음 페이지가 남아 있으면 hasNext가 참이다")
    void 다음_페이지_있음() {
        // given
        given(productSearchRepository.search(any(), anyInt(), anyInt()))
            .willReturn(new ProductSearchPage(List.of(hit(1L)), 45L, 0L));

        // when
        final ProductSearchResult result = productSearchService.searchProducts("장기하", null, 0, 20);

        // then
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지에서는 hasNext가 거짓이다")
    void 마지막_페이지() {
        // given
        given(productSearchRepository.search(any(), anyInt(), anyInt()))
            .willReturn(new ProductSearchPage(List.of(hit(1L)), 45L, 0L));

        // when
        final ProductSearchResult result = productSearchService.searchProducts("장기하", null, 2, 20);

        // then
        // 45건을 20개씩 나누면 2페이지(0,1,2)가 마지막이다
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("조회 가능한 범위를 넘어선 페이지는 조회하지 않고 빈 결과를 돌려준다")
    void 범위_밖_페이지는_빈_결과() {
        // given
        // 검색엔진이 한 질의로 훑을 수 있는 건수는 기본 10,000건이다. 500페이지 * 20건 = 10,020건이라
        // 그대로 조회하면 요청이 거절돼 500으로 나간다
        final int page = 500;

        // when
        final ProductSearchResult result = productSearchService.searchProducts("장기하", null, page, 20);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isZero();
        assertThat(result.hasNext()).isFalse();
        verify(productSearchRepository, never()).search(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("범위 경계 바로 안쪽 페이지는 그대로 조회한다")
    void 경계_안쪽_페이지는_조회() {
        // given
        // 499페이지 * 20건 = 정확히 10,000건으로 아직 조회할 수 있다. 여기까지 막으면 볼 수 있는 결과를 잃는다
        given(productSearchRepository.search(any(), anyInt(), anyInt()))
            .willReturn(ProductSearchPage.empty());

        // when
        productSearchService.searchProducts("장기하", null, 499, 20);

        // then
        verify(productSearchRepository).search(any(), eq(499), eq(20));
    }

    private ProductSearchHit hit(final Long productId) {
        return new ProductSearchHit(productId, "별일 없이 산다", "장기하와 얼굴들", null, null, null);
    }

    @Test
    @DisplayName("검색어의 연속 공백을 정리해 리포지토리에 넘긴다")
    void 검색어_정리_후_전달() {
        // given
        given(productSearchRepository.search(any(), anyInt(), anyInt()))
            .willReturn(ProductSearchPage.empty());
        final ArgumentCaptor<SearchKeyword> captor = ArgumentCaptor.forClass(SearchKeyword.class);

        // when
        productSearchService.searchProducts("  장기하와   얼굴들 ", null, 0, 20);

        // then
        verify(productSearchRepository).search(captor.capture(), anyInt(), anyInt());
        assertThat(captor.getValue().getValue()).isEqualTo("장기하와 얼굴들");
    }

    @Test
    @DisplayName("대상을 안 주면 이름 검색 경로로 간다")
    void 대상_미지정_이름검색() {
        // given
        given(productSearchRepository.search(any(SearchKeyword.class), anyInt(), anyInt()))
                .willReturn(new ProductSearchPage(List.of(), 0L, 0L));

        // when
        productSearchService.searchProducts("장기하", null, 0, 20);

        // then
        // 이 갈래가 뒤집히면 type을 안 보내던 기존 프론트 호출이 전부 번호 검색으로 샌다
        then(productSearchRepository).should().search(any(SearchKeyword.class), anyInt(), anyInt());
        then(productSearchRepository).should(never())
                .searchByCatalogNumber(any(SearchKeyword.class), anyInt(), anyInt());
    }

    @Test
    @DisplayName("카탈로그 대상이면 번호 조회로 간다")
    void 카탈로그_대상_번호조회() {
        // given
        given(productSearchRepository.searchByCatalogNumber(any(SearchKeyword.class), anyInt(), anyInt()))
                .willReturn(new ProductSearchPage(List.of(), 0L, 0L));

        // when
        productSearchService.searchProducts("BLP-1567", "catalog", 0, 20);

        // then
        then(productSearchRepository).should()
                .searchByCatalogNumber(any(SearchKeyword.class), anyInt(), anyInt());
        then(productSearchRepository).should(never()).search(any(SearchKeyword.class), anyInt(), anyInt());
    }

    @Test
    @DisplayName("모르는 대상은 조회하지 않고 거절한다")
    void 미지원_대상_거절() {
        // given
        // when & then
        assertThatThrownBy(() -> productSearchService.searchProducts("BLP-1567", "catlog", 0, 20))
                .isInstanceOf(UnsupportedSearchTargetException.class);
        then(productSearchRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("번호로 대조할 글자가 없으면 조회하지 않고 빈 결과를 준다")
    void 정규화_값_없으면_빈_결과() {
        // given — 기호만 있는 검색어. 짧지는 않아서 길이 규칙에는 안 걸린다
        // when
        final ProductSearchResult result = productSearchService.searchProducts("--", "catalog", 0, 20);

        // then
        // 조회를 막지 않으면 판매중인 상품 전체가 번호 검색 결과로 나온다
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        then(productSearchRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("번호로 대조할 글자가 한 자뿐이면 조회하지 않고 빈 결과를 준다")
    void 정규화_값이_짧으면_빈_결과() {
        // given — 기호를 걷어내면 한 글자만 남는다. 원문은 세 글자라 길이 규칙에는 안 걸린다
        // when
        final ProductSearchResult result = productSearchService.searchProducts("--a", "catalog", 0, 20);

        // then
        // 한 글자로 앞부분 일치를 걸면 번호 대부분이 걸려 전체 건수까지 세게 된다
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        then(productSearchRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("결과가 0건인 검색도 기록한다")
    void 결과가_0건이어도_기록한다() {
        // given
        final String tooShortKeyword = "a";

        // when
        productSearchService.searchProducts(tooShortKeyword, null, 0, 20);

        // then
        // 결과가 없는 검색어야말로 가장 알고 싶은 기록이다. 조회를 건너뛰는 경로도 남아야 한다
        final ArgumentCaptor<SearchLogCommand> captor = ArgumentCaptor.forClass(SearchLogCommand.class);
        then(searchLogService).should().saveSearchLog(captor.capture());
        assertThat(captor.getValue().resultCount()).isZero();
        assertThat(captor.getValue().keyword()).isEqualTo(tooShortKeyword);
    }

    @Test
    @DisplayName("범위를 넘어선 페이지 요청도 기록한다")
    void 범위_밖_페이지도_기록한다() {
        // given
        final int page = 500;

        // when
        productSearchService.searchProducts("장기하", null, page, 20);

        // then
        final ArgumentCaptor<SearchLogCommand> captor = ArgumentCaptor.forClass(SearchLogCommand.class);
        then(searchLogService).should().saveSearchLog(captor.capture());
        assertThat(captor.getValue().page()).isEqualTo(page);
        assertThat(captor.getValue().resultCount()).isZero();
    }

    @Test
    @DisplayName("응답에 실리는 식별자와 기록에 남는 식별자가 같다")
    void 응답과_기록의_식별자가_같다() {
        // given
        given(productSearchRepository.search(any(), anyInt(), anyInt()))
            .willReturn(new ProductSearchPage(List.of(hit(1L)), 45L, 7L));

        // when
        final ProductSearchResult result = productSearchService.searchProducts("장기하", null, 0, 20);

        // then
        // 이 값이 어긋나면 클릭 기록을 검색 기록에 이어 붙일 수 없다
        final ArgumentCaptor<SearchLogCommand> captor = ArgumentCaptor.forClass(SearchLogCommand.class);
        then(searchLogService).should().saveSearchLog(captor.capture());
        assertThat(result.searchId()).isNotBlank();
        assertThat(captor.getValue().searchId()).isEqualTo(result.searchId());
    }

    @Test
    @DisplayName("검색 엔진이 알려준 처리 시간을 기록에 담는다")
    void 엔진_처리시간을_기록한다() {
        // given
        given(productSearchRepository.search(any(), anyInt(), anyInt()))
            .willReturn(new ProductSearchPage(List.of(hit(1L)), 45L, 7L));

        // when
        productSearchService.searchProducts("장기하", null, 0, 20);

        // then
        final ArgumentCaptor<SearchLogCommand> captor = ArgumentCaptor.forClass(SearchLogCommand.class);
        then(searchLogService).should().saveSearchLog(captor.capture());
        assertThat(captor.getValue().engineMillis()).isEqualTo(7L);
    }

    @Test
    @DisplayName("검색어가 없어 예외를 던질 때는 기록하지 않는다")
    void 예외를_던지면_기록하지_않는다() {
        // given
        final String keyword = null;

        // when & then
        assertThatThrownBy(() -> productSearchService.searchProducts(keyword, null, 0, 20))
            .isInstanceOf(SearchKeywordRequiredException.class);
        then(searchLogService).shouldHaveNoInteractions();
    }
}
