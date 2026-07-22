package site.coreservice.product.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import site.common.response.ApiResponse;
import site.coreservice.product.domain.ProductNotFoundException;

class ProductExceptionHandlerTest {

    private final ProductExceptionHandler handler = new ProductExceptionHandler();

    @Test
    @DisplayName("상품없음 예외를 404와 PERR-4100 실패 응답으로 변환한다")
    void handleProductNotFoundException_404와_PERR4100으로_변환() {
        // given
        ProductNotFoundException e = new ProductNotFoundException();

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleProductNotFoundException(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
        // ApiResponse.Error가 common 패키지 전용이라 직렬화 형태로 검증한다 (실제 응답 payload와 동일)
        String json = new ObjectMapper().writeValueAsString(response.getBody());
        assertThat(json).contains("\"PERR-4100\"").contains("상품을 찾을 수 없습니다");
    }
}
