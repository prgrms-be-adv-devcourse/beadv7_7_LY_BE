package site.productservice.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.productservice.domain.ProductAlias;

public interface ProductAliasJpaRepository extends JpaRepository<ProductAlias, Long> {

    boolean existsByProductIdAndNormalizedName(Long productId, String normalizedName);

    List<ProductAlias> findByProductIdIn(List<Long> productIds);
}
