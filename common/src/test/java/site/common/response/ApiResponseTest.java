package site.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.common.exception.GlobalErrorCode;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * ApiResponse의 JSON 왕복(내보내기 → 되읽기) 검증. 되읽는 쪽은 다른 서비스의 응답을 받는
 * HTTP 클라이언트들인데 전부 local이 아닐 때만 실행되는 코드라, 이 왕복이 깨져도 로컬에서는
 * 드러나지 않는다 — 그래서 여기서 상시 검증한다. (#118: @JsonCreator가 없어 되읽기가 불가능했던 결함)
 */
class ApiResponseTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private record TestPayload(Long id, String name) {
    }

    @Test
    @DisplayName("성공 응답은 JSON으로 내보냈다가 다시 읽어도 값이 같다")
    void 성공_응답_왕복() {
        // given
        ApiResponse<TestPayload> original = ApiResponse.success(new TestPayload(7L, "Abbey Road"));

        // when
        String json = objectMapper.writeValueAsString(original);
        ApiResponse<TestPayload> restored = objectMapper.readValue(json,
                new TypeReference<ApiResponse<TestPayload>>() {
                });

        // then
        assertThat(restored.isSuccess()).isTrue();
        assertThat(restored.getData()).isEqualTo(new TestPayload(7L, "Abbey Road"));
        assertThat(restored.getError().code()).isNull();
        assertThat(restored.getError().message()).isNull();
    }

    @Test
    @DisplayName("실패 응답은 에러 코드와 메시지가 그대로 되살아난다")
    void 실패_응답_왕복() {
        // given
        ApiResponse<Void> original = ApiResponse.fail(GlobalErrorCode.RESOURCE_NOT_FOUND);

        // when
        String json = objectMapper.writeValueAsString(original);
        ApiResponse<Void> restored = objectMapper.readValue(json, new TypeReference<ApiResponse<Void>>() {
        });

        // then
        assertThat(restored.isSuccess()).isFalse();
        assertThat(restored.getData()).isNull();
        assertThat(restored.getError().code()).isEqualTo(GlobalErrorCode.RESOURCE_NOT_FOUND.getValue());
        assertThat(restored.getError().message()).isEqualTo(GlobalErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("데이터 없는 성공 응답도 왕복이 된다")
    void 데이터_없는_성공_응답_왕복() {
        // given
        ApiResponse<Void> original = ApiResponse.success();

        // when
        String json = objectMapper.writeValueAsString(original);
        ApiResponse<Void> restored = objectMapper.readValue(json, new TypeReference<ApiResponse<Void>>() {
        });

        // then
        assertThat(restored.isSuccess()).isTrue();
        assertThat(restored.getData()).isNull();
    }
}
