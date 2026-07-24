package site.common.response;

import lombok.Getter;
import site.common.exception.ErrorCode;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final Error error;

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(
            true,
            null,
            Error.empty()
        );
    }

    public static <T> ApiResponse<T> success(final T data) {
        return new ApiResponse<>(
            true,
            data,
            Error.empty()
        );
    }

    public static ApiResponse<Void> fail(final ErrorCode errorCode) {
        return new ApiResponse<>(
            false,
            null,
            new Error(errorCode.getValue(), errorCode.getMessage())
        );
    }

    private ApiResponse(
        final boolean success,
        final T data,
        final Error error
    ) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    record Error(String code, String message) {
        static Error empty() {
            return new Error(null, null);
        }
    }
}
