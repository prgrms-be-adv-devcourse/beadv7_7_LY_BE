package site.explorationservice.ai.embedding.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Service;
import site.explorationservice.ai.embedding.application.dto.EmbeddingResult;

/**
 * 텍스트를 벡터로 변환하는 유일한 창구.
 * <p>
 * 검색과 추천이 각자 임베딩을 계산하면 같은 상품에 대해 OpenAI 호출이 중복되고 같은 필드(content_vector)를 두고 경쟁하게 되므로, 계산 경로는 이 클래스
 * 하나로만 유지한다.
 * <p>
 * Spring AI의 EmbeddingModel 자체가 공급자 교체 가능한 추상화라, 그 위에 별도 Port 인터페이스를 두지 않는다.
 * <p>
 * 배치 메서드를 처음부터 두는 이유는, OpenAI 임베딩 API가 원래 텍스트 배열을 받아 한 번의 호출로 벡터 배열을 반환하기 때문이다 — 위시리스트 클러스터를 여러 개
 * 임베딩할 때 호출 횟수를 늘리지 않아도 된다.
 */
@RequiredArgsConstructor
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * 모델·차원을 호출 시점에 덮어쓴다. 어떤 조합을 쓸지 확정하기 전에 재기동 없이 비교하기 위한 경로다 (예: text-embedding-3-small의 1536 vs
     * 512). 둘 다 null이면 설정값을 그대로 쓴다.
     */
    public EmbeddingResult embed(final List<String> texts, final String model,
        final Integer dimensions) {
        final EmbeddingResponse response = call(texts, model, dimensions);

        final List<float[]> vectors = response.getResults().stream()
            .map(Embedding::getOutput)
            .toList();
        final Usage usage = response.getMetadata().getUsage();

        return new EmbeddingResult(
            vectors,
            response.getMetadata().getModel(),
            usage.getPromptTokens(),
            usage.getTotalTokens()
        );
    }

    private EmbeddingResponse call(final List<String> texts, final String model,
        final Integer dimensions) {
        if (model == null && dimensions == null) {
            return embeddingModel.embedForResponse(texts);
        }

        final OpenAiEmbeddingOptions.Builder options = OpenAiEmbeddingOptions.builder();
        if (model != null) {
            options.model(model);
        }
        if (dimensions != null) {
            options.dimensions(dimensions);
        }
        return embeddingModel.call(new EmbeddingRequest(texts, options.build()));
    }
}
