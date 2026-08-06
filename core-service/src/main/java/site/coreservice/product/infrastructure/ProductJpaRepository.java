package site.coreservice.product.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.search.ProductSearchHit;

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

    // 주의: 검색 쿼리와 count 쿼리는 join·where 조건이 항상 같아야 한다 — 한쪽만 고치면 totalElements가 조용히 틀어진다
    @Query("""
            select new site.coreservice.product.domain.search.ProductSearchHit(
                    p.id, p.title, a.name, p.coverImage, p.releaseYear, p.pressType, p.releaseCountry)
            from Product p join Artist a on a.id = p.artistId
            where p.active = true
              and (p.normalizedTitle like :pattern
                   or a.normalizedName like :pattern
                   or exists (select 1 from ProductAlias pa
                              where pa.productId = p.id and pa.normalizedName like :pattern)
                   or exists (select 1 from ArtistAlias aa
                              where aa.artistId = p.artistId and aa.normalizedName like :pattern))
            order by p.id asc
            """)
    List<ProductSearchHit> searchActiveHits(@Param("pattern") String pattern, Pageable pageable);

    @Query("""
            select count(p)
            from Product p join Artist a on a.id = p.artistId
            where p.active = true
              and (p.normalizedTitle like :pattern
                   or a.normalizedName like :pattern
                   or exists (select 1 from ProductAlias pa
                              where pa.productId = p.id and pa.normalizedName like :pattern)
                   or exists (select 1 from ArtistAlias aa
                              where aa.artistId = p.artistId and aa.normalizedName like :pattern))
            """)
    long countActiveHits(@Param("pattern") String pattern);

    // 주의: 목록 쿼리와 count 쿼리는 join·where 조건이 항상 같아야 한다 — 한쪽만 고치면 totalElements가 조용히 틀어진다
    // (지금은 조건이 active 하나뿐이지만, 아티스트 조인이 양쪽에 다 있어야 수가 맞는다)
    @Query("""
            select new site.coreservice.product.domain.search.ProductSearchHit(
                    p.id, p.title, a.name, p.coverImage, p.releaseYear, p.pressType, p.releaseCountry)
            from Product p join Artist a on a.id = p.artistId
            where p.active = true
            order by p.id desc
            """)
    List<ProductSearchHit> findActiveHits(Pageable pageable);

    @Query("""
            select count(p)
            from Product p join Artist a on a.id = p.artistId
            where p.active = true
            """)
    long countActive();
}
