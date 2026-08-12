package site.productservice.application.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.productservice.application.dto.search.ProductSearchResult;
import site.productservice.domain.search.ProductSearchPage;
import site.productservice.domain.search.ProductSearchRepository;
import site.productservice.domain.search.SearchKeyword;
import site.productservice.exception.SearchKeywordRequiredException;

/** 상품 검색(명세 1-1). 제목·제목 별칭·아티스트명·아티스트 별칭 어디에 걸려도 상품 카드가 나온다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSearchService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int MIN_KEYWORD_LENGTH = 2;

    private final ProductSearchRepository productSearchRepository;

    public ProductSearchResult searchProducts(String keyword, int page, int size) {
        validateKeyword(keyword);
        int safePage = Math.max(page, 0);
        int safeSize = clampSize(size);
        SearchKeyword searchKeyword = SearchKeyword.from(keyword);
        // 너무 짧은 검색어는 잘못된 요청이 아니라 "결과 없음"으로 취급한다 — 프론트 응답 계약이 바뀌지 않도록
        if (searchKeyword.isEmpty() || searchKeyword.getWhole().length() < MIN_KEYWORD_LENGTH) {
            return ProductSearchResult.empty(safePage, safeSize);
        }
        ProductSearchPage searchPage = productSearchRepository.searchActiveByKeyword(searchKeyword, safePage,
                safeSize);
        return ProductSearchResult.of(searchPage, safePage, safeSize);
    }

    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new SearchKeywordRequiredException();
        }
    }

    private int clampSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
