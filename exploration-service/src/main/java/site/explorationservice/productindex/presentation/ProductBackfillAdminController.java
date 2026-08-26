package site.explorationservice.productindex.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.explorationservice.productindex.application.ProductBackfillService;
import site.explorationservice.productindex.application.dto.BackfillResult;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.presentation.dto.BackfillResponse;

/**
 * product-service를 커서로 순회하며 실제 상품을 색인한다 — 운영 인덱스를 채우는 용도라 {@code @Profile("local")}로 막지 않는다.
 * <b>동기 호출이고 maxProducts만큼만 처리하고 멈춘다</b> — 전체를 한 번에 돌리는 게 아니라, 응답의 nextCursor를
 * 다음 호출의 startCursor에 넣어 나눠서 반복 호출하는 걸 전제로 한다. maxProducts를 크게 잡을수록 응답이 오래 걸리고 OpenAI 임베딩 호출 비용도
 * 그만큼 든다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/exploration/products/backfill")
public class ProductBackfillAdminController {

    private final ProductBackfillService productBackfillService;
    private final ElasticsearchOperations elasticsearchOperations;

    @PostMapping
    public ApiResponse<BackfillResponse> backfill(
        @RequestParam(required = false) final Long startCursor,
        @RequestParam(defaultValue = "10000") final int maxProducts) {
        final BackfillResult result = productBackfillService.backfill(startCursor, maxProducts);

        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        return ApiResponse.success(BackfillResponse.from(result));
    }
}
