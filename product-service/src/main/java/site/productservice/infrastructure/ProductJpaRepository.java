package site.productservice.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.productservice.domain.PressType;
import site.productservice.domain.Product;

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

    /**
     * 아티스트를 조인하지 않는다. 아티스트에서 아무 값도 읽지 않아 조인이 거르는 일만 하기 때문이다.
     * 가리키는 아티스트가 없는 상품이 하나도 없으므로 빼도 수가 같고, 43만 행마다 아티스트를 찾아보는
     * 비용만 사라진다.
     * <p>
     * <b>이 전제는 데이터베이스가 강제하지 않는다.</b> 상품이 아티스트를 외래 키가 아니라 식별자 값으로만 참조해서,
     * 짝이 없는 상품이 없다는 것은 지금 적재된 데이터가 그렇다는 사실일 뿐이다. 없는 아티스트를 가리키는 상품이
     * 생기면 목록에는 안 보이는데 전체 건수에는 잡히게 된다. 두 숫자가 어긋나면 여기를 먼저 의심할 것.
     */
    @Query("""
            select count(p)
            from Product p
            where p.active = true
            """)
    long countActive();

    @Query("""
            select p from Product p
            where :cursor is null or p.id > :cursor
            order by p.id asc
            """)
    List<Product> findAllOrderByIdAfter(@Param("cursor") Long cursor, Pageable pageable);
}
