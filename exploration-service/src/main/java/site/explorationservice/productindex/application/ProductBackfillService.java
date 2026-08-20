package site.explorationservice.productindex.application;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.explorationservice.productindex.application.dto.BackfillResult;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;
import site.explorationservice.productindex.application.port.ProductPort;
import site.explorationservice.productindex.application.port.dto.ProductPage;

/**
 * product-service를 커서로 순회하며 상품을 색인한다. 지금은 동기다 — 50만 건 전체를 한 호출로 끝내는 게 아니라, maxProducts만큼만 처리하고 멈추는
 * 식으로 나눠서 호출하는 걸 전제로 한다. 정확성을 먼저 검증하는 단계라 비동기·재개 인프라(상태 조회 엔드포인트 등) 없이 시작한다 — 전체를 무인으로 돌릴 시점이 오면 그때
 * 비동기로 바꾼다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductBackfillService {

    /**
     * product-service에서 한 번에 가져오는 상품 수이자, 임베딩 API 한 번에 태우는 상품 수이기도 하다. 임베딩 배치가 크면 그중 하나만 문제가 있어도
     * 전체가 실패하므로(블라스트 반경), 요청 횟수와 실패 반경 사이의 절충으로 정한 값이다.
     */
    private static final int PAGE_SIZE = 100;

    private final ProductPort productPort;
    private final ProductIndexService productIndexService;

    /**
     * @param startCursor 시작 지점. null이면 처음부터
     * @param maxProducts 이번 호출에서 처리할 상한 — 페이지 경계에서 멈추므로 실제로는 이 값 이상으로 넘어갈 수 있다(마지막 페이지를 끝까지 처리하기
     *                    때문)
     */
    public BackfillResult backfill(final Long startCursor, final int maxProducts,
        final ProductEmbeddingTemplate template) {
        Long cursor = startCursor;
        Long resumeCursor = startCursor;
        int totalIndexed = 0;
        final List<Long> failedProductIds = new ArrayList<>();

        while (totalIndexed < maxProducts) {
            final ProductPage page = productPort.findAllProducts(cursor, PAGE_SIZE);
            if (page.items().isEmpty()) {
                resumeCursor = null;
                break;
            }

            totalIndexed += indexPage(page, template, failedProductIds);
            log.info("백필 진행 — {}건 처리, 실패 {}건, 사용 cursor: {}", totalIndexed,
                failedProductIds.size(), cursor);

            resumeCursor = page.hasNext() ? page.nextCursor() : null;
            if (!page.hasNext()) {
                break;
            }
            cursor = page.nextCursor();
        }

        return new BackfillResult(totalIndexed, resumeCursor, failedProductIds);
    }

    /**
     * 페이지 하나가 실패해도(임베딩 API 오류 등) 백필 전체를 멈추지 않는다 — 실패한 상품 id를 모아두고 다음 페이지로 계속 진행한다. 크래시로 이 결과 자체가
     * 유실돼도 복구할 수 있게 실패마다 로그를 남긴다.
     */
    private int indexPage(final ProductPage page, final ProductEmbeddingTemplate template,
        final List<Long> failedProductIds) {
        try {
            productIndexService.indexAll(page.items(), template);
            return page.items().size();
        } catch (final RuntimeException e) {
            final List<Long> pageProductIds =
                page.items().stream().map(ProductIndexCommand::productId).toList();
            failedProductIds.addAll(pageProductIds);
            log.error("백필 페이지 실패, 다음 페이지로 계속 진행 — productIds: {}", pageProductIds, e);
            return 0;
        }
    }
}
