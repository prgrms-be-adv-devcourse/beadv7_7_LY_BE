package site.explorationservice.ai.chat.application;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

/**
 * 구조화 출력으로 챗 모델을 호출하는 유일한 창구.
 * <p>
 * <b>여기서 하는 건 "형식이 깨지지 않게 받아오는 것"까지다.</b> 프롬프트에 {@link BeanOutputConverter}의 형식 지시문을 붙이고,
 * 응답을 그 타입으로 파싱해서 돌려준다. 파싱된 값이 <em>의미상</em> 말이 되는지(예: 가중치가 음수는 아닌지)는 호출자의 바운디드 컨텍스트가 판단할 일이라 여기서
 * 검증하지 않는다.
 * <p>
 * 도메인 예외로도 감싸지 않는다 — {@code EmbeddingService}도 그렇듯, 실패(호출 실패든 파싱 실패든)를 어떤 예외로 바꿀지는 호출자가 결정한다.
 * Spring AI/파싱 예외를 그대로 전파한다.
 * <p>
 * temperature는 호출자가 매번 {@link ChatTemperature}로 명시한다 — 구조화된 답이 필요한 작업(흔들림이 적어야 함)과 자유도가 필요한 작업의
 * 적정값이 다르므로 기본값을 여기서 고정하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModel chatModel;

    public <T> T call(final String prompt, final Class<T> responseType,
        final ChatTemperature temperature) {
        final BeanOutputConverter<T> converter = new BeanOutputConverter<>(responseType);
        final String fullPrompt = prompt + "\n\n" + converter.getFormat();
        final ChatOptions options =
            OpenAiChatOptions.builder().temperature(temperature.getValue()).build();

        final ChatResponse response = chatModel.call(new Prompt(fullPrompt, options));
        return converter.convert(response.getResult().getOutput().getText());
    }
}
