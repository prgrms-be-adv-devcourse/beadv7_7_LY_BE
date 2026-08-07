package site.productservice.infrastructure.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.productservice.domain.Artist;
import site.productservice.domain.ArtistAlias;
import site.productservice.domain.PressType;
import site.productservice.domain.Product;
import site.productservice.domain.ProductAlias;
import site.productservice.domain.search.ProductSearchHit;
import site.productservice.domain.search.ProductSearchPage;
import site.productservice.domain.search.ProductSearchRepository;
import site.productservice.infrastructure.ArtistAliasJpaRepository;
import site.productservice.infrastructure.ArtistJpaRepository;
import site.productservice.infrastructure.ProductAliasJpaRepository;
import site.productservice.infrastructure.ProductJpaRepository;
import site.productservice.support.RepositoryTest;

@RepositoryTest
@Import(ProductSearchRepositoryImpl.class)
class ProductSearchRepositoryImplTest {

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ArtistJpaRepository artistJpaRepository;

    @Autowired
    private ArtistAliasJpaRepository artistAliasJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductAliasJpaRepository productAliasJpaRepository;

    private Long saveArtist(String name, String... aliases) {
        Long artistId = artistJpaRepository.save(Artist.of(name)).getId();
        for (String alias : aliases) {
            artistAliasJpaRepository.save(ArtistAlias.of(artistId, alias));
        }
        return artistId;
    }

    private Product saveProduct(Long artistId, String catalogNumber, String title, String... titleAliases) {
        Product product = productJpaRepository.save(Product.of(catalogNumber, artistId, title, "UK", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null));
        for (String alias : titleAliases) {
            productAliasJpaRepository.save(ProductAlias.of(product.getId(), alias));
        }
        return product;
    }

    @Test
    @DisplayName("정규화된 제목 부분일치로 찾는다")
    void search_제목_매칭() {
        // given
        Long beatles = saveArtist("The Beatles");
        saveProduct(beatles, "PCS 7088", "Abbey Road");

        // when
        ProductSearchPage result = productSearchRepository.searchActiveByKeyword("abbeyroad", 0, 20);

        // then
        assertThat(result.content()).extracting(ProductSearchHit::title).containsExactly("Abbey Road");
        assertThat(result.content()).extracting(ProductSearchHit::artistName).containsExactly("The Beatles");
    }

    @Test
    @DisplayName("제목 별칭(한글)으로 찾는다")
    void search_제목_별칭_매칭() {
        // given
        Long beatles = saveArtist("The Beatles");
        saveProduct(beatles, "PCS 7088", "Abbey Road", "애비 로드");

        // when
        ProductSearchPage result = productSearchRepository.searchActiveByKeyword("애비로드", 0, 20);

        // then
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("아티스트명으로 찾으면 그 아티스트의 상품이 나온다")
    void search_아티스트명_매칭() {
        // given
        Long beatles = saveArtist("The Beatles");
        saveProduct(beatles, "PCS 7088", "Abbey Road");

        // when
        ProductSearchPage result = productSearchRepository.searchActiveByKeyword("thebeatles", 0, 20);

        // then
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("아티스트 별칭(한글)으로 찾는다")
    void search_아티스트_별칭_매칭() {
        // given
        Long beatles = saveArtist("The Beatles", "비틀즈");
        saveProduct(beatles, "PCS 7088", "Abbey Road");

        // when
        ProductSearchPage result = productSearchRepository.searchActiveByKeyword("비틀즈", 0, 20);

        // then
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("비활성 상품은 결과에서 제외한다")
    void search_비활성_제외() {
        // given
        Long beatles = saveArtist("The Beatles");
        Product product = saveProduct(beatles, "PCS 7088", "Abbey Road");
        product.deactivate();
        productJpaRepository.save(product);

        // when
        ProductSearchPage result = productSearchRepository.searchActiveByKeyword("abbeyroad", 0, 20);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 아티스트를 가리키는 상품은 결과에서 제외한다 (데이터 오류 방어)")
    void search_고아_아티스트_제외() {
        // given — 아티스트 없이 상품만 저장
        saveProduct(999L, "PCS 7088", "Abbey Road");

        // when
        ProductSearchPage result = productSearchRepository.searchActiveByKeyword("abbeyroad", 0, 20);

        // then
        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("제목과 별칭에 동시에 걸려도 결과 행은 중복되지 않는다")
    void search_다중_매칭_중복_없음() {
        // given — "abbeyroad"가 제목과 제목 별칭 모두에 걸리게
        Long beatles = saveArtist("The Beatles", "비틀즈");
        saveProduct(beatles, "PCS 7088", "Abbey Road", "Abbey Road 1969");

        // when
        ProductSearchPage result = productSearchRepository.searchActiveByKeyword("abbeyroad", 0, 20);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("페이징이 productId 순서로 안정적이고 count와 일치한다")
    void search_페이징과_count() {
        // given
        Long beatles = saveArtist("The Beatles");
        saveProduct(beatles, "A1", "Abbey Road");
        saveProduct(beatles, "A2", "Abbey Road");
        saveProduct(beatles, "A3", "Abbey Road");

        // when
        ProductSearchPage firstPage = productSearchRepository.searchActiveByKeyword("abbeyroad", 0, 2);
        ProductSearchPage secondPage = productSearchRepository.searchActiveByKeyword("abbeyroad", 1, 2);

        // then
        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(3L);
    }
}
