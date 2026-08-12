package site.explorationservice.ai.embedding.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.explorationservice.ai.embedding.application.dto.EmbeddingResult;
import site.explorationservice.ai.embedding.application.EmbeddingService;
import site.explorationservice.ai.embedding.presentation.dto.EmbeddingProbeRequest;
import site.explorationservice.ai.embedding.presentation.dto.EmbeddingProbeResponse;

/**
 * OpenAI 연결과 임베딩 차원 수를 직접 확인하기 위한 창구. 검증이 끝나면 지우거나 실제 API로 대체된다 (임베딩 계산 자체는 EmbeddingService에 있고
 * 그쪽이 계속 남는다).
 * <p>
 *
 * @Profile("local")이 붙어 있는 건 필수다 — 임의의 텍스트를 받아 우리 계정으로 OpenAI에 보내는 통로라, 운영에 딸려가면 외부인이 무료 프록시로 쓸 수
 * 있고 청구는 그대로 온다. 로컬에서만 빈이 생성되게 막는다.
 */
@Profile("local")
@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/v1/exploration/ai")
public class EmbeddingProbeController {

    private final EmbeddingService embeddingService;

    @PostMapping("/embedding")
    public ApiResponse<EmbeddingProbeResponse> embed(
        @RequestBody final EmbeddingProbeRequest request) {
        final long startedAt = System.nanoTime();
        final EmbeddingResult result = embeddingService.embed(
            request.texts(), request.model(), request.dimensions());
        final long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        return ApiResponse.success(EmbeddingProbeResponse.from(result, elapsedMs));
    }
}
