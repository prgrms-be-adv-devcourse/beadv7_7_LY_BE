package site.common.exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.retry.RetryException;
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
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(final BusinessException e) {
        log.error("{} 발생!", e.getClass().getSimpleName(), e);

        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(fail(errorCode));
    }

    /**
     * 스프링 프레임워크 7의 @Retryable(core 내장 재시도)이 재시도를 다 소진하면, 원래 예외를
     * 그대로 던지지 않고 이 RetryException으로 감싸서 던진다(마지막 예외는 cause에 담김). 그대로
     * 두면 원래 BusinessException이 여기서 안 잡히고 catch-all(handleException, GERR-0001)로
     * 뭉개지니, cause를 풀어서 원래 처리 경로로 되돌린다.
     */
    @ExceptionHandler(RetryException.class)
    public ResponseEntity<ApiResponse<Void>> handleRetryException(final RetryException e) {
        if (e.getCause() instanceof BusinessException businessException) {
            log.warn("재시도 소진 - {}", businessException.getClass().getSimpleName(), e);
            return handleBusinessException(businessException);
        }
        log.error("재시도 소진 - 알 수 없는 원인", e);
        return ResponseEntity.status(INTERNAL_SERVER_APPLICATION_ERROR.getStatus())
                .body(fail(INTERNAL_SERVER_APPLICATION_ERROR));
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