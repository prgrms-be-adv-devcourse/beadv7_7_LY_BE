package site.explorationservice.search.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.common.exception.GlobalExceptionHandler;
import site.explorationservice.search.application.ProductSearchService;
import site.explorationservice.search.application.dto.ProductSearchResult;
import site.explorationservice.search.domain.ProductSearchHit;
import site.explorationservice.search.exception.SearchKeywordRequiredException;

/**
 * 검색을 이 서비스로 옮기는 게 목적이라 나가는 응답이 상품 서비스의 것과 같아야 한다. 필드 이름 하나만 달라져도
 * 프론트가 깨지므로, 계약을 사람 눈이 아니라 이 테스트가 붙잡아 둔다.
 */
@WebMvcTest(ProductSearchController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("상품 검색 API")
class ProductSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductSearchService productSearchService;

    @Test
    @DisplayName("검색에 성공하면 200과 약속된 필드 이름으로 응답한다")
    void 검색_응답_형식() throws Exception {
        // given
        final ProductSearchHit hit = new ProductSearchHit(42L, "별일 없이 산다", "장기하와 얼굴들",
            "https://img.example.com/42.jpg", 2009, "ORIGINAL");
        given(productSearchService.searchProducts(anyString(), anyInt(), anyInt()))
            .willReturn(new ProductSearchResult(List.of(hit), 0, 20, 45L, true));

        // when & then
        mockMvc.perform(get("/api/v1/search/products").param("q", "장기하"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].productId").value(42))
            .andExpect(jsonPath("$.data.content[0].title").value("별일 없이 산다"))
            .andExpect(jsonPath("$.data.content[0].artistName").value("장기하와 얼굴들"))
            .andExpect(jsonPath("$.data.content[0].coverImageUrl").value("https://img.example.com/42.jpg"))
            .andExpect(jsonPath("$.data.content[0].releaseYear").value(2009))
            .andExpect(jsonPath("$.data.content[0].pressType").value("ORIGINAL"))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(45))
            .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("페이지 정보를 생략하면 0페이지 20건으로 조회한다")
    void 페이지_기본값() throws Exception {
        // given
        given(productSearchService.searchProducts(anyString(), anyInt(), anyInt()))
            .willReturn(new ProductSearchResult(List.of(), 0, 20, 0L, false));
        final ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

        // when
        mockMvc.perform(get("/api/v1/search/products").param("q", "장기하"))
            .andExpect(status().isOk());

        // then
        // 기본값이 바뀌면 프론트가 첫 화면에 받는 건수가 달라진다
        verify(productSearchService).searchProducts(eq("장기하"), pageCaptor.capture(), sizeCaptor.capture());
        assertThat(pageCaptor.getValue()).isZero();
        assertThat(sizeCaptor.getValue()).isEqualTo(20);
    }

    @Test
    @DisplayName("검색어 없이 호출하면 400과 약속된 에러 코드로 응답한다")
    void 검색어_누락() throws Exception {
        // given
        // 서비스가 검색어를 검증하고 그 예외를 공통 핸들러가 400으로 바꾼다. 이 경로가 끊기면
        // 프레임워크 예외가 그대로 500으로 새어 나가 프론트가 코드로 분기할 수 없다
        given(productSearchService.searchProducts(isNull(), anyInt(), anyInt()))
            .willThrow(new SearchKeywordRequiredException());

        // when & then
        mockMvc.perform(get("/api/v1/search/products"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("PERR-4001"));
    }
}
