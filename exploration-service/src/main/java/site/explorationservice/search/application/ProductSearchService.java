package site.explorationservice.search.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.explorationservice.search.application.dto.ProductSearchResult;
import site.explorationservice.search.domain.ProductSearchPage;
import site.explorationservice.search.domain.ProductSearchRepository;
import site.explorationservice.search.domain.SearchKeyword;
import site.explorationservice.search.domain.SearchTarget;
import site.explorationservice.search.exception.SearchKeywordRequiredException;

/**
 * 검색어 검증과 페이지 보정만 하고 조회는 리포지토리에 맡긴다.
 * <p>
 * 트랜잭션 애너테이션을 붙이지 않는다 — 이 서비스는 데이터베이스를 쓰지 않는다. exploration-service는 아예
 * DataSource 자동설정을 꺼둔 상태다.
 */
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /**
     * Elasticsearch는 한 질의가 훑을 수 있는 결과 범위를 인덱스당 기본 10,000건으로 제한한다. 건너뛸 건수와 가져올 건수의
     * 합이 이걸 넘으면 조회 자체가 실패한다.
     */
    private static final long MAX_RESULT_WINDOW = 10_000L;

    private final ProductSearchRepository productSearchRepository;

    public ProductSearchResult searchProducts(final String keyword, final String searchBy, final int page,
            final int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new SearchKeywordRequiredException();
        }

        final SearchTarget searchTarget = SearchTarget.from(searchBy);
        final int safePage = Math.max(page, 0);
        final int safeSize = clampSize(size);
        final SearchKeyword searchKeyword = SearchKeyword.from(keyword);

        // 너무 짧은 검색어는 잘못된 요청이 아니라 "결과 없음"으로 다룬다 — 응답 형식이 그대로 유지되도록
        if (searchKeyword.isTooShort()) {
            return ProductSearchResult.empty(safePage, safeSize);
        }

        // 조회 가능한 범위를 넘어선 페이지도 같은 방식으로 다룬다. 그대로 조회하면 검색엔진이 요청을 거절하고
        // 그 예외가 500으로 나가는데, 옮겨오기 전 데이터베이스 조회는 같은 요청에 빈 목록을 돌려줬다
        if (isBeyondResultWindow(safePage, safeSize)) {
            return ProductSearchResult.empty(safePage, safeSize);
        }

        if (searchTarget == SearchTarget.CATALOG) {
            return searchByCatalogNumber(searchKeyword, safePage, safeSize);
        }

        final ProductSearchPage searchPage = productSearchRepository.search(searchKeyword, safePage, safeSize);
        return ProductSearchResult.of(searchPage, safePage, safeSize);
    }

    private ProductSearchResult searchByCatalogNumber(final SearchKeyword keyword, final int page, final int size) {
        // 표기를 통일하고 나면 한 글자만 남을 수 있다. 그 한 글자로 앞부분 일치를 걸면 번호 대부분이 걸린다
        if (keyword.isNormalizedTooShort()) {
            return ProductSearchResult.empty(page, size);
        }

        final ProductSearchPage searchPage = productSearchRepository.searchByCatalogNumber(keyword, page, size);
        return ProductSearchResult.of(searchPage, page, size);
    }

    private boolean isBeyondResultWindow(final int page, final int size) {
        // long으로 계산한다. int로 두면 페이지 번호가 커질 때 곱셈에서 넘쳐 판정이 뒤집힌다
        return (page + 1L) * size > MAX_RESULT_WINDOW;
    }

    private int clampSize(final int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
