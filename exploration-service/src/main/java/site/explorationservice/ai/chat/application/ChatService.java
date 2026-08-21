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
 * {@link site.explorationservice.ai.embedding.application.EmbeddingService}와 대칭이다 — 검색·추천이 각자 챗 호출을
 * 짜면 배선이 중복되므로, 계산 경로는 이 클래스 하나로만 유지한다.
 * <p>
 * <b>프롬프트 지시문이 아니라 OpenAI의 네이티브 Structured Outputs(스키마 강제)를 쓴다.</b>
 * {@code converter.getFormat()}을 프롬프트에 끼워 넣는 방식은 모델이 형식을 지키려고 "노력"할 뿐 API가 강제하지 않는다 — 실제로 형식이 깨지는
 * 사례가 나올 수 있다는 전제로 {@code InterestWeightService}가 검증 로직을 따로 두고 있었다. {@code outputSchema}로 넘기면 API
 * 레벨에서 스키마를 강제하므로 이 클래스에서 "형식이 깨지지 않게 받아오는 것"을 훨씬 신뢰성 있게 보장한다.
 * <p>
 * 파싱된 값이 <em>의미상</em> 말이 되는지(예: 가중치가 음수는 아닌지)는 호출자의 바운디드 컨텍스트가 판단할 일이라 여기서 검증하지 않는다.
 * <p>
 * 도메인 예외로도 감싸지 않는다 — {@code EmbeddingService}도 그렇듯, 실패(호출 실패든 파싱 실패든)를 어떤 예외로 바꿀지는 호출자가 결정한다.
 * Spring AI/파싱 예외를 그대로 전파한다.
 * <p>
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModel chatModel;

    public <T> T call(final String prompt, final Class<T> responseType) {
        final BeanOutputConverter<T> converter = new BeanOutputConverter<>(responseType);
        final ChatOptions options =
            OpenAiChatOptions.builder().outputSchema(converter.getJsonSchema()).build();

        final ChatResponse response = chatModel.call(new Prompt(prompt, options));
        return converter.convert(response.getResult().getOutput().getText());
    }
}
