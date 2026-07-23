package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.product.domain.ProductAlias;
import site.coreservice.product.domain.ProductAliasRepository;

@Repository
@RequiredArgsConstructor
public class ProductAliasRepositoryImpl implements ProductAliasRepository {

    private final ProductAliasJpaRepository productAliasJpaRepository;

    @Override
    public ProductAlias save(ProductAlias alias) {
        return productAliasJpaRepository.save(alias);
    }

    @Override
    public boolean hasAlias(Long productId, String normalizedName) {
        return productAliasJpaRepository.existsByProductIdAndNormalizedName(productId, normalizedName);
    }
}
