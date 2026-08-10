package site.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import site.common.response.ApiResponse;

import static site.common.exception.GlobalErrorCode.INTERNAL_SERVER_APPLICATION_ERROR;
import static site.common.exception.GlobalErrorCode.RESOURCE_NOT_FOUND;
import static site.common.response.ApiResponse.fail;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(final BusinessException e) {
        log.error("{} 발생!", e.getClass().getSimpleName(), e);

        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(fail(errorCode));
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(final NoResourceFoundException e) {
        log.warn("{} 발생!", e.getClass().getSimpleName(), e);
        return ResponseEntity.status(RESOURCE_NOT_FOUND.getStatus()).body(fail(RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleException(final Exception e) {
        log.error("{} 발생!", e.getClass().getSimpleName(), e);
        return ResponseEntity.status(INTERNAL_SERVER_APPLICATION_ERROR.getStatus()).body(fail(INTERNAL_SERVER_APPLICATION_ERROR));
    }
}
