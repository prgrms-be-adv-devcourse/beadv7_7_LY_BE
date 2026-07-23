package site.coreservice.product.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNormalizedCatalogNumberAndFormatAndReleaseCountry(
            String normalizedCatalogNumber, String format, String releaseCountry);

    @Query("""
            select p from Product p
            where p.normalizedCatalogNumber is null
              and p.normalizedTitle = :normalizedTitle
              and p.artistId = :artistId
              and p.releaseYear = :releaseYear
              and p.releaseCountry = :releaseCountry
              and p.format = :format
              and p.pressType = :pressType
            """)
    Optional<Product> findByFallbackNaturalKey(@Param("normalizedTitle") String normalizedTitle,
            @Param("artistId") Long artistId, @Param("releaseYear") int releaseYear,
            @Param("releaseCountry") String releaseCountry, @Param("format") String format,
            @Param("pressType") PressType pressType);
}
