package site.explorationservice.productindex.application;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.common.text.TextNormalizer;
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

    public ProductIndexResult index(final ProductIndexCommand command) {
        return indexAll(List.of(command)).getFirst();
    }

    /**
     * 여러 상품을 <b>한 번의 임베딩 호출</b>로 색인한다. 상품 하나당 텍스트 3개(identity·origin·edition)를 만들지만, OpenAI 임베딩
     * API가 텍스트 배열을 받아 벡터 배열을 반환하므로 호출은 여전히 한 번이다 — 텍스트 종류별로 블록을 나눠 한 번에 보내고, 응답을 같은 순서로 다시 3등분해 상품에
     * 붙인다. 기존 상품을 일괄 색인할 때 쓰인다.
     * <p>
     * 저장도 한 번에 보낸다 — 문서 하나씩 요청을 보내면 건수만큼 왕복이 생긴다.
     * <p>
     * {@code ProductEmbeddingTemplate}(COMPACT/LABELED)로 만들던 단일 결합 텍스트는 더 이상 임베딩하지 않는다 — 그 벡터를 저장하던
     * {@code contentVector} 필드가 3벡터 구조로 대체되면서 제거됐다.
     */
    public List<ProductIndexResult> indexAll(final List<ProductIndexCommand> commands) {
        final int n = commands.size();
        final List<String> identityTexts =
            commands.stream().map(ProductEmbeddingTemplate::buildIdentity).toList();
        final List<String> originTexts =
            commands.stream().map(ProductEmbeddingTemplate::buildOrigin).toList();
        final List<String> editionTexts =
            commands.stream().map(ProductEmbeddingTemplate::buildEdition).toList();

        final List<String> allTexts = new ArrayList<>(n * 3);
        allTexts.addAll(identityTexts);
        allTexts.addAll(originTexts);
        allTexts.addAll(editionTexts);

        final EmbeddingResult embedding = embeddingService.embed(allTexts, null, null);
        final List<float[]> vectors = embedding.vectors();
        final List<float[]> identityVectors = vectors.subList(0, n);
        final List<float[]> originVectors = vectors.subList(n, 2 * n);
        final List<float[]> editionVectors = vectors.subList(2 * n, 3 * n);

        final List<ProductDocument> documents = IntStream.range(0, n)
            .mapToObj(i -> toDocument(commands.get(i),
                identityTexts.get(i), originTexts.get(i), editionTexts.get(i),
                identityVectors.get(i), originVectors.get(i), editionVectors.get(i)))
            .toList();
        productDocumentRepository.saveAll(documents);

        log.info("상품 색인 완료 — {}건, 차원: {}, 모델: {}, 토큰: {}",
            n, embedding.dimensions(), embedding.model(), embedding.totalTokens());

        return IntStream.range(0, n)
            .mapToObj(i -> new ProductIndexResult(
                commands.get(i).productId(),
                identityTexts.get(i),
                originTexts.get(i),
                editionTexts.get(i),
                embedding.dimensions(),
                embedding.model(),
                embedding.totalTokens()))
            .toList();
    }

    private ProductDocument toDocument(final ProductIndexCommand command,
        final String identityText, final String originText, final String editionText,
        final float[] identityVector, final float[] originVector, final float[] editionVector) {
        return ProductDocument.builder()
            .productId(command.productId())
            .title(command.title())
            .artistName(command.artistName())
            // 값이 없으면 살아 있는 것으로 본다 — 색인 대상으로 들어온 상품이 기본적으로 노출 가능한 상태라고
            // 보는 게 맞고, null이면 active 필터에 걸려 추천에서 통째로 빠지기 때문이다.
            .active(command.active() == null || command.active())
            .coverImageUrl(command.coverImageUrl())
            .genre(command.genre())
            .label(command.label())
            .releaseYear(command.releaseYear())
            .releaseCountry(command.releaseCountry())
            .pressType(command.pressType())
            // 원본은 화면에 보여주기 위한 것이고, 대조는 표기를 다듬은 값으로 한다.
            // 검색어를 다듬을 때와 같은 함수를 써야 색인된 값과 검색 키가 맞는다.
            .catalogNumber(command.catalogNumber())
            .normalizedCatalogNumber(TextNormalizer.normalize(command.catalogNumber()))
            .groupKey(ProductDocument.groupKeyOf(command.discogsMasterId(), command.productId()))
            // 검색이 다른 표기로 찾을 때 쓴다. 임베딩 텍스트에는 들어가지 않으므로 벡터에 영향이 없다
            .titleAliases(command.titleAliases())
            .artistAliases(command.artistAliases())
            // 그룹 키 = 임베딩 텍스트 그대로. 별도 포맷이 필요 없는 이유는 ProductDocument의 그룹 키 필드 주석 참고.
            .identityGroupKey(identityText)
            .originGroupKey(originText)
            .editionGroupKey(editionText)
            .identityVector(identityVector)
            .originVector(originVector)
            .editionVector(editionVector)
            .build();
    }
}
