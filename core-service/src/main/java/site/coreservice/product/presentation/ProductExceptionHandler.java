package site.coreservice.product.presentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.common.response.ApiResponse;
import site.coreservice.product.domain.ProductNotFoundException;

/**
 * 임시 product 전용 예외 핸들러 — common 예외 구조(#48) merge 후 이 파일을 삭제하고
 * ProductNotFoundException을 BusinessException 상속으로 교체하면 common 핸들러가 대체한다.
 * common GlobalExceptionHandler(LOWEST_PRECEDENCE)의 Exception 핸들러보다 먼저 잡히도록 우선순위를 높인다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "site.coreservice.product")
public class ProductExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleProductNotFoundException(final ProductNotFoundException e) {
        log.warn("{} 발생!", e.getClass().getSimpleName(), e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(e.getErrorCode()));
    }
}
