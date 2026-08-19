package site.explorationservice.productindex.application;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.explorationservice.ai.embedding.application.EmbeddingService;
import site.explorationservice.ai.embedding.application.dto.EmbeddingResult;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;
import site.explorationservice.productindex.application.dto.ProductIndexResult;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.domain.ProductDocumentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final EmbeddingService embeddingService;
    private final ProductDocumentRepository productDocumentRepository;

    public ProductIndexResult index(final ProductIndexCommand command,
        final ProductEmbeddingTemplate template) {
        return indexAll(List.of(command), template).getFirst();
    }

    /**
     * 여러 상품을 <b>한 번의 임베딩 호출</b>로 색인한다. 상품 하나당 텍스트 4개(combined·identity·origin·edition)를 만들지만,
     * OpenAI 임베딩 API가 텍스트 배열을 받아 벡터 배열을 반환하므로 호출은 여전히 한 번이다 — 텍스트 종류별로 블록을 나눠 한 번에 보내고, 응답을 같은 순서로
     * 다시 4등분해 상품에 붙인다. 기존 상품을 일괄 색인할 때 쓰인다.
     * <p>
     * 저장도 한 번에 보낸다 — 문서 하나씩 요청을 보내면 건수만큼 왕복이 생긴다.
     */
    public List<ProductIndexResult> indexAll(final List<ProductIndexCommand> commands,
        final ProductEmbeddingTemplate template) {
        final int n = commands.size();
        final List<String> combinedTexts = commands.stream().map(template::build).toList();
        final List<String> identityTexts =
            commands.stream().map(ProductEmbeddingTemplate::buildIdentity).toList();
        final List<String> originTexts =
            commands.stream().map(ProductEmbeddingTemplate::buildOrigin).toList();
        final List<String> editionTexts =
            commands.stream().map(ProductEmbeddingTemplate::buildEdition).toList();

        final List<String> allTexts = new ArrayList<>(n * 4);
        allTexts.addAll(combinedTexts);
        allTexts.addAll(identityTexts);
        allTexts.addAll(originTexts);
        allTexts.addAll(editionTexts);

        final EmbeddingResult embedding = embeddingService.embed(allTexts, null, null);
        final List<float[]> vectors = embedding.vectors();
        final List<float[]> combinedVectors = vectors.subList(0, n);
        final List<float[]> identityVectors = vectors.subList(n, 2 * n);
        final List<float[]> originVectors = vectors.subList(2 * n, 3 * n);
        final List<float[]> editionVectors = vectors.subList(3 * n, 4 * n);

        final List<ProductDocument> documents = IntStream.range(0, n)
            .mapToObj(i -> toDocument(commands.get(i), combinedVectors.get(i),
                identityVectors.get(i), originVectors.get(i), editionVectors.get(i)))
            .toList();
        productDocumentRepository.saveAll(documents);

        log.info("상품 색인 완료 — {}건, 차원: {}, 모델: {}, 토큰: {}",
            n, embedding.dimensions(), embedding.model(), embedding.totalTokens());

        return IntStream.range(0, n)
            .mapToObj(i -> new ProductIndexResult(
                commands.get(i).productId(),
                combinedTexts.get(i),
                identityTexts.get(i),
                originTexts.get(i),
                editionTexts.get(i),
                embedding.dimensions(),
                embedding.model(),
                embedding.totalTokens()))
            .toList();
    }

    private ProductDocument toDocument(final ProductIndexCommand command,
        final float[] contentVector, final float[] identityVector, final float[] originVector,
        final float[] editionVector) {
        return ProductDocument.builder()
            .productId(command.productId())
            .title(command.title())
            .artistName(command.artistName())
            // 값이 없으면 살아 있는 것으로 본다 — 색인 대상으로 들어온 상품이 기본적으로 노출 가능한 상태라고
            // 보는 게 맞고, null이면 active 필터에 걸려 추천에서 통째로 빠지기 때문이다.
            .active(command.active() == null || command.active())
            .contentVector(contentVector)
            .identityVector(identityVector)
            .originVector(originVector)
            .editionVector(editionVector)
            .build();
    }
}
