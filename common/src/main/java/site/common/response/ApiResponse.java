package site.common.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    // @JsonCreator: 다른 서비스가 보낸 응답 JSON을 받아 객체로 되읽을 때 Jackson이 쓸 생성 경로.
    // 이 표시가 없으면 내보내기(직렬화)만 되고 받아 읽기(역직렬화)는 예외가 난다 — private 생성자는 후보에도 안 오른다
    @JsonCreator
    private ApiResponse(
        @JsonProperty("success") final boolean success,
        @JsonProperty("data") final T data,
        @JsonProperty("error") final Error error
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
