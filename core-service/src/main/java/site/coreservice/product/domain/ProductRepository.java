package site.coreservice.product.domain;

import java.util.Optional;

/**
 * 상품 저장소 (도메인 인터페이스).
 * 구현체는 infrastructure의 ProductRepositoryImpl.
 */
public interface ProductRepository {

    Product save(Product product);

    /** dedup 자연키로 조회 (정규화된_카탈로그넘버 + 포맷 + 발매국가). 시드 멱등성 확보용. */
    Optional<Product> findByNaturalKey(String normalizedCatalogNumber, String format, String releaseCountry);
}
