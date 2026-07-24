package site.coreservice.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.product.application.dto.ProductSearchResult;
import site.coreservice.product.domain.ProductSearchPage;
import site.coreservice.product.domain.ProductSearchRepository;
import site.coreservice.product.domain.TextNormalizer;
import site.coreservice.product.exception.SearchKeywordRequiredException;

/** 상품 검색(명세 1-1). 제목·제목 별칭·아티스트명·아티스트 별칭 어디에 걸려도 상품 카드가 나온다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSearchService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ProductSearchRepository productSearchRepository;

    public ProductSearchResult searchProducts(String keyword, int page, int size) {
        validateKeyword(keyword);
        int safePage = Math.max(page, 0);
        int safeSize = clampSize(size);
        String normalizedKeyword = TextNormalizer.normalize(keyword);
        if (normalizedKeyword == null) {
            return ProductSearchResult.empty(safePage, safeSize);
        }
        ProductSearchPage searchPage = productSearchRepository.searchActiveByKeyword(normalizedKeyword, safePage,
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
