package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.ProductRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findByNaturalKey(
            String normalizedCatalogNumber, String format, String releaseCountry) {
        return productJpaRepository.findByNormalizedCatalogNumberAndFormatAndReleaseCountry(
                normalizedCatalogNumber, format, releaseCountry);
    }

    @Override
    public Optional<Product> findByFallbackNaturalKey(String normalizedTitle, Long artistId, int releaseYear,
            String releaseCountry, String format, PressType pressType) {
        return productJpaRepository.findByFallbackNaturalKey(normalizedTitle, artistId, releaseYear, releaseCountry,
                format, pressType);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public List<Product> findAllByIds(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }
}
