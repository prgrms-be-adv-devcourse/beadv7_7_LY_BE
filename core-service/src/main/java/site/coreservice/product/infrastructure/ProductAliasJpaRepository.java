package site.coreservice.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.product.domain.ProductAlias;

public interface ProductAliasJpaRepository extends JpaRepository<ProductAlias, Long> {

    boolean existsByProductIdAndNormalizedName(Long productId, String normalizedName);
}
