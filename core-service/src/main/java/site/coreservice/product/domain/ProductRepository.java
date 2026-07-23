package site.coreservice.product.domain;

import java.util.Optional;

/**
 * 상품 저장소 (도메인 인터페이스).
 * 구현체는 infrastructure의 ProductRepositoryImpl.
 */
public interface ProductRepository {

    Product save(Product product);

    /**
     * 중복 확인 기준(정규화 카탈로그번호 + 포맷 + 발매국가)으로 조회한다.
     * 시드를 여러 번 실행해도 같은 상품이 두 번 저장되지 않게 하는 데 쓴다.
     */
    Optional<Product> findByNaturalKey(String normalizedCatalogNumber, String format, String releaseCountry);

    Optional<Product> findById(Long id);
}
