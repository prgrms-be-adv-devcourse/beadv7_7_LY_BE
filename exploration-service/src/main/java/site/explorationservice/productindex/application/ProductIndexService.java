package site.explorationservice.productindex.application;

import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import site.explorationservice.ai.embedding.application.EmbeddingService;
import site.explorationservice.ai.embedding.application.dto.EmbeddingResult;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;
import site.explorationservice.productindex.application.dto.ProductIndexResult;
import site.explorationservice.productindex.domain.ProductDocument;

//테스트용
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final EmbeddingService embeddingService;
    private final ElasticsearchOperations elasticsearchOperations;

    public ProductIndexResult index(final ProductIndexCommand command,
        final ProductEmbeddingTemplate template) {
        return indexAll(List.of(command), template).getFirst();
    }

    /**
     * 여러 상품을 <b>한 번의 임베딩 호출</b>로 색인한다. OpenAI 임베딩 API가 원래 텍스트 배열을 받아 벡터 배열을 반환하므로, 상품 수만큼 호출을 반복할
     * 이유가 없다. 기존 상품을 일괄 색인할 때 쓰인다.
     * <p>
     * 저장도 한 번에 보낸다 — 문서 하나씩 요청을 보내면 건수만큼 왕복이 생긴다.
     */
    public List<ProductIndexResult> indexAll(final List<ProductIndexCommand> commands,
        final ProductEmbeddingTemplate template) {
        final List<String> embeddedTexts = commands.stream()
            .map(template::build)
            .toList();
        final EmbeddingResult embedding = embeddingService.embed(embeddedTexts, null, null);

        final List<ProductDocument> documents = IntStream.range(0, commands.size())
            .mapToObj(i -> toDocument(commands.get(i), embedding.vectors().get(i)))
            .toList();
        elasticsearchOperations.save(documents);

        log.info("상품 색인 완료 — {}건, 차원: {}, 모델: {}, 토큰: {}",
            commands.size(), embedding.dimensions(), embedding.model(), embedding.totalTokens());

        return IntStream.range(0, commands.size())
            .mapToObj(i -> new ProductIndexResult(
                commands.get(i).productId(),
                embeddedTexts.get(i),
                embedding.dimensions(),
                embedding.model(),
                embedding.totalTokens()))
            .toList();
    }

    private ProductDocument toDocument(final ProductIndexCommand command, final float[] vector) {
        return ProductDocument.builder()
            .productId(command.productId())
            .title(command.title())
            .artistName(command.artistName())
            // 값이 없으면 살아 있는 것으로 본다 — 색인 대상으로 들어온 상품이 기본적으로 노출 가능한 상태라고
            // 보는 게 맞고, null이면 active 필터에 걸려 추천에서 통째로 빠지기 때문이다.
            .active(command.active() == null || command.active())
            .contentVector(vector)
            .build();
    }
}
