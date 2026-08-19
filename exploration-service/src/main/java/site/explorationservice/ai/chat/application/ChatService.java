package site.explorationservice.ai.chat.application;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/**
 * 텍스트로 챗 모델을 호출하는 유일한 창구.
 * {@link site.explorationservice.ai.embedding.application.EmbeddingService}와 대칭이다 — 검색·추천이 각자 챗 호출을
 * 짜면 배선이 중복되므로, 계산 경로는 이 클래스 하나로만 유지한다.
 * <p>
 * 도메인 예외로 감싸지 않는다 — {@code EmbeddingService}도 그렇듯, 실패를 어떤 예외로 바꿀지는 호출자의 바운디드 컨텍스트가 결정할 일이라 Spring
 * AI 예외를 그대로 전파한다.
 * <p>
 * 구조화 출력 파싱(예: {@code BeanOutputConverter})은 여기서 하지 않는다 — 호출자마다 원하는 응답 타입이 달라서, 프롬프트에 형식 지시문을 넣고
 * 응답을 해석하는 건 각 도메인 서비스의 몫이다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModel chatModel;

    public String call(final String prompt) {
        return chatModel.call(new Prompt(prompt)).getResult().getOutput().getText();
    }
}
