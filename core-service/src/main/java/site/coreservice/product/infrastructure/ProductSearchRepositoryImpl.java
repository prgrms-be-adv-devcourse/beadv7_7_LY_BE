package site.coreservice.product.infrastructure;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import site.coreservice.product.domain.ProductSearchHit;
import site.coreservice.product.domain.ProductSearchPage;
import site.coreservice.product.domain.ProductSearchRepository;

/**
 * LIKE 기반 검색 구현. Spring Data의 Pageable·Page는 이 클래스 안에서만 쓰고,
 * 도메인 계약(ProductSearchPage)으로 옮겨 담아 반환한다 — 기술 타입이 인터페이스 밖으로 새지 않게.
 */
@Repository
@RequiredArgsConstructor
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public ProductSearchPage searchActiveByKeyword(String normalizedKeyword, int page, int size) {
        String pattern = "%" + normalizedKeyword + "%";
        List<ProductSearchHit> hits = productJpaRepository.searchActiveHits(pattern, PageRequest.of(page, size));
        long totalElements = productJpaRepository.countActiveHits(pattern);
        return new ProductSearchPage(hits, totalElements);
    }
}
