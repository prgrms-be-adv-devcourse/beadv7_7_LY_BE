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
        final ErrorCode errorCode = e.getErrorCode();
        // 4xx의 경우 클라이언트 요청 문제로 보고 Warn
        final LogLevel logLevel =
            errorCode.getStatus().is4xxClientError() ? LogLevel.WARN : LogLevel.ERROR;

        logException(logLevel, errorCode, e);

        return ResponseEntity.status(errorCode.getStatus()).body(fail(errorCode));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
        final NoResourceFoundException e) {
        logException(LogLevel.WARN, RESOURCE_NOT_FOUND, e);

        return ResponseEntity.status(RESOURCE_NOT_FOUND.getStatus()).body(fail(RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(final Exception e) {
        logException(LogLevel.ERROR, INTERNAL_SERVER_APPLICATION_ERROR, e);

        return ResponseEntity.status(INTERNAL_SERVER_APPLICATION_ERROR.getStatus())
            .body(fail(INTERNAL_SERVER_APPLICATION_ERROR));
    }

    private void logException(final LogLevel logLevel, final ErrorCode errorCode,
        final Throwable e) {
        final String message = String.format("[type=%s] [code=%s] [status=%d] message=\"%s\"",
            e.getClass().getSimpleName(),
            errorCode.getValue(),
            errorCode.getStatus().value(),
            e.getMessage());

        switch (logLevel) {
            case WARN -> log.warn(message, e);
            case ERROR -> log.error(message, e);
        }
    }

    private enum LogLevel {
        WARN, ERROR
    }
}