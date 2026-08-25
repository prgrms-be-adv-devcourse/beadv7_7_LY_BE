package site.explorationservice.ai.chat.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import site.explorationservice.ai.chat.domain.ChatCallGate;

/**
 * 게이트가 호출 경계에서 실제로 막아주는지만 본다 — 정상 호출(파싱 포함)은 Spring AI 내부 동작이라 여기서 다시 검증하지 않는다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("챗 서비스")
class ChatServiceTest {

    private record Dummy(String value) {

    }

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatCallGate chatCallGate;

    @InjectMocks
    private ChatService sut;

    @Test
    @DisplayName("게이트가 닫혀 있으면 ChatModel을 아예 부르지 않고 예외를 던진다")
    void 게이트_닫힘() {
        given(chatCallGate.isOpen()).willReturn(false);

        assertThatThrownBy(() -> sut.call("prompt", Dummy.class))
            .isInstanceOf(IllegalStateException.class);

        then(chatModel).should(never()).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    }

    @Test
    @DisplayName("게이트가 열려 있으면 ChatModel 호출을 시도한다")
    void 게이트_열림() {
        given(chatCallGate.isOpen()).willReturn(true);

        // ChatModel을 더 스텁하지 않으면 null 응답을 파싱하다 NPE가 나는데, 그 자체가 "호출을
        // 시도했다"는 증거다 — 게이트가 열려 있을 때의 정상 파싱 경로는 여기서 검증할 범위가 아니다.
        assertThatThrownBy(() -> sut.call("prompt", Dummy.class))
            .isInstanceOf(NullPointerException.class);

        then(chatModel).should().call(any(org.springframework.ai.chat.prompt.Prompt.class));
    }
}
