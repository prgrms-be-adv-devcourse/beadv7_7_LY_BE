package site.coreservice.product.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.product.domain.Product;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNormalizedCatalogNumberAndFormatAndReleaseCountry(
            String normalizedCatalogNumber, String format, String releaseCountry);
}
