package site.explorationservice.ai.chat.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.explorationservice.ai.chat.domain.ChatCallGate;
import site.explorationservice.ai.chat.presentation.dto.ChatCallStatusResponse;

/**
 * 챗 모델(OpenAI) 호출을 수동으로 켜고 끈다 — 장애가 길어질 때, 이 서비스의 모든 챗 호출부를 막는 장애 대응용 스위치.
 */
@RestController
@RequestMapping("/api/admin/v1/exploration/ai/chat")
@RequiredArgsConstructor
public class ChatCallAdminController {

    private final ChatCallGate chatCallGate;

    @GetMapping
    public ApiResponse<ChatCallStatusResponse> status() {
        return ApiResponse.success(new ChatCallStatusResponse(chatCallGate.isOpen()));
    }

    @PostMapping("/close")
    public ApiResponse<Void> close() {
        chatCallGate.close();
        return ApiResponse.success();
    }

    @PostMapping("/open")
    public ApiResponse<Void> open() {
        chatCallGate.open();
        return ApiResponse.success();
    }
}
