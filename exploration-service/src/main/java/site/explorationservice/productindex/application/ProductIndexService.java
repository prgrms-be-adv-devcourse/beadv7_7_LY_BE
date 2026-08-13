package site.explorationservice.productindex.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import site.explorationservice.ai.embedding.application.EmbeddingService;
import site.explorationservice.ai.embedding.application.dto.EmbeddingResult;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;
import site.explorationservice.productindex.application.dto.ProductIndexResult;
import site.explorationservice.productindex.domain.ProductDocument;

/**
 * 상품을 임베딩해서 ES에 색인한다.
 * <p>
 * 지금은 수동 트리거가 호출하지만 <b>이 클래스는 남는다</b> — 나중에 상품 변경 이벤트를 받는 리스너가 그대로 호출한다. 버려지는 건 그 위의 컨트롤러다.
 * <p>
 * <b>재색인은 자연히 멱등하다.</b> ProductDocument의 문서 id가 productId라 같은 상품을 여러 번 색인해도
 * 덮어쓰기가 되고 중복이 생기지 않는다 — at-least-once 전제의 리스너를 붙일 때 별도 처리가 필요 없다.
 * <p>
 * 저장에 리포지토리 대신 ElasticsearchOperations를 직접 쓰는 건, 하는 일이 save 한 줄이라 인터페이스를 하나 더 두는 값이 없고 나중에 kNN 조회를
 * 붙일 때도 결국 Operations가 필요하기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final EmbeddingService embeddingService;
    private final ElasticsearchOperations elasticsearchOperations;

    public ProductIndexResult index(final ProductIndexCommand command,
        final ProductEmbeddingTemplate template) {
        final String embeddedText = template.build(command);
        final EmbeddingResult embedding = embeddingService.embed(List.of(embeddedText), null, null);

        elasticsearchOperations.save(toDocument(command, embedding.first()));

        log.info("상품 색인 완료 — productId: {}, 차원: {}, 모델: {}",
            command.productId(), embedding.dimensions(), embedding.model());

        return new ProductIndexResult(
            command.productId(),
            embeddedText,
            embedding.dimensions(),
            embedding.model(),
            embedding.totalTokens()
        );
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
