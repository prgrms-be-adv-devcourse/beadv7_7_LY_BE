package site.explorationservice.productindex.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.explorationservice.productindex.application.ProductEmbeddingTemplate;
import site.explorationservice.productindex.application.ProductIndexService;
import site.explorationservice.productindex.application.dto.ProductIndexResult;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.presentation.dto.IndexedProductResponse;
import site.explorationservice.productindex.presentation.dto.ProductIndexProbeRequest;
import site.explorationservice.productindex.presentation.dto.ProductIndexProbeResponse;

/**
 * 상품 색인을 손으로 실행해 보기 위한 창구. 상품 변경 이벤트를 구독하기 전에 <b>임베딩과 색인 경로를 먼저 확정해 두려는 것</b>이다 — 이벤트를 바로 붙이면 잘못됐을
 * 때 배선·임베딩·색인 중 어디가 문제인지 뒤섞인다. 검증이 끝나면 지워지고, 색인 로직 자체는 ProductIndexService에 남는다.
 * <p>
 * <b>@Profile("local")이 필수다.</b> 임의의 상품 정보를 받아 우리 계정으로 OpenAI를 호출하고 운영 인덱스에
 * 쓰는 통로라, 운영에 딸려가면 비용과 데이터 양쪽이 노출된다.
 * <p>
 * 색인 로직이 아닌 ES 조회를 여기서 직접 하는 건 <b>진단용이기 때문</b>이다 — "이 문서에 벡터가 들어갔나", "방금 넣은 걸 바로 보이게 해달라"는 확인 편의이지
 * ProductIndexService가 가질 책임이 아니다. 어차피 지워질 코드라 영구 클래스를 오염시키지 않는 쪽을 택했다.
 */
@Profile("local")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/exploration/index/products")
public class ProductIndexProbeController {

    private final ProductIndexService productIndexService;
    private final ElasticsearchOperations elasticsearchOperations;

    @PostMapping
    public ApiResponse<ProductIndexProbeResponse> index(
        @RequestBody final ProductIndexProbeRequest request) {
        final ProductEmbeddingTemplate template = request.templateOrDefault();

        final long startedAt = System.nanoTime();
        final ProductIndexResult result =
            productIndexService.index(request.toCommand(), template);
        final long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        // 손으로 넣고 바로 확인할 수 있게 강제로 refresh한다. ES는 기본적으로 1초 주기로 refresh하므로
        // 운영 경로에서는 필요 없고, 그래서 ProductIndexService가 아니라 여기서만 한다.
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        return ApiResponse.success(
            ProductIndexProbeResponse.from(result, template, elapsedMs));
    }

    @GetMapping("/{productId}")
    public ApiResponse<IndexedProductResponse> find(@PathVariable final Long productId) {
        final ProductDocument document = elasticsearchOperations.get(
            String.valueOf(productId), ProductDocument.class);

        if (document == null) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(
            IndexedProductResponse.of(document, hasVector(productId)));
    }

    /**
     * 벡터가 _source에 안 담기므로 값으로는 확인할 수 없다. 대신 그 필드에 값이 있는 문서만 매칭되는 exists 질의로 색인 여부를 판정한다.
     */
    private boolean hasVector(final Long productId) {
        final NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.bool(b -> b
                .filter(f -> f.term(t -> t.field("productId").value(productId)))
                .filter(f -> f.exists(e -> e.field("contentVector")))))
            .build();

        return elasticsearchOperations.count(query, ProductDocument.class) > 0;
    }
}
