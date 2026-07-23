package site.coreservice.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.ProductRepository;
import site.coreservice.support.RepositoryTest;

@RepositoryTest
@Import(ProductRepositoryImpl.class)
class ProductRepositoryImplTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("예비 기준 조회는 카탈로그번호 있는 상품과 충돌하지 않는다 (번호 없는 행끼리만 비교)")
    void findByFallbackNaturalKey_번호_있는_상품과_불충돌() {
        // given — 같은 제목·아티스트·연도·국가·포맷·프레스의 "번호 있는" 정품만 존재
        productJpaRepository.save(Product.of("PCS 7088", 1L, "Abbey Road", "UK", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null));

        // when
        Optional<Product> result = productRepository.findByFallbackNaturalKey("abbeyroad", 1L, 1969,
                "UK", "LP", PressType.ORIGINAL);

        // then — 정품이 걸리면 부틀렉이 영영 적재되지 못한다
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("예비 기준 조회는 번호 없는 같은 상품을 찾는다")
    void findByFallbackNaturalKey_번호_없는_상품_매칭() {
        // given
        productJpaRepository.save(Product.of(null, 1L, "Kum Back", "US", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null));

        // when
        Optional<Product> result = productRepository.findByFallbackNaturalKey("kumback", 1L, 1969,
                "US", "LP", PressType.ORIGINAL);

        // then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("발매국가가 다르면 번호 없는 상품이라도 다른 상품이다")
    void findByFallbackNaturalKey_국가_다르면_미매칭() {
        // given
        productJpaRepository.save(Product.of(null, 1L, "Kum Back", "US", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null));

        // when
        Optional<Product> result = productRepository.findByFallbackNaturalKey("kumback", 1L, 1969,
                "UK", "LP", PressType.ORIGINAL);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("포맷이 다르면 번호 없는 상품이라도 다른 상품이다")
    void findByFallbackNaturalKey_포맷_다르면_미매칭() {
        // given
        productJpaRepository.save(Product.of(null, 1L, "Kum Back", "US", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null));

        // when
        Optional<Product> result = productRepository.findByFallbackNaturalKey("kumback", 1L, 1969,
                "US", "180g", PressType.ORIGINAL);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("프레스구분이 다르면 번호 없는 상품이라도 다른 상품이다")
    void findByFallbackNaturalKey_프레스구분_다르면_미매칭() {
        // given
        productJpaRepository.save(Product.of(null, 1L, "Kum Back", "US", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null));

        // when
        Optional<Product> result = productRepository.findByFallbackNaturalKey("kumback", 1L, 1969,
                "US", "LP", PressType.REISSUE);

        // then
        assertThat(result).isEmpty();
    }
}
