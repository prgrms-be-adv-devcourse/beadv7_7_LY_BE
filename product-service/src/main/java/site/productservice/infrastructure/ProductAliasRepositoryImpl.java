package site.productservice.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.productservice.domain.ProductAlias;
import site.productservice.domain.ProductAliasRepository;

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

    @Override
    public Map<Long, List<String>> findNamesByProductIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productAliasJpaRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(ProductAlias::getProductId,
                        Collectors.mapping(ProductAlias::getName, Collectors.toList())));
    }
}
