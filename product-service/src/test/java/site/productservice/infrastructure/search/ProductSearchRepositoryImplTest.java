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
import site.productservice.domain.search.SearchKeyword;
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

    private ProductSearchPage search(String raw, int page, int size) {
        return productSearchRepository.searchActiveByKeyword(SearchKeyword.from(raw), page, size);
    }

    @Test
    @DisplayName("정규화된 제목 부분일치로 찾는다")
    void search_제목_매칭() {
        // given
        Long beatles = saveArtist("The Beatles");
        saveProduct(beatles, "PCS 7088", "Abbey Road");

        // when
        ProductSearchPage result = search("abbeyroad", 0, 20);

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
        ProductSearchPage result = search("애비로드", 0, 20);

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
        ProductSearchPage result = search("thebeatles", 0, 20);

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
        ProductSearchPage result = search("비틀즈", 0, 20);

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
        ProductSearchPage result = search("abbeyroad", 0, 20);

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
        ProductSearchPage result = search("abbeyroad", 0, 20);

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
        ProductSearchPage result = search("abbeyroad", 0, 20);

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
        ProductSearchPage firstPage = search("abbeyroad", 0, 2);
        ProductSearchPage secondPage = search("abbeyroad", 1, 2);

        // then
        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("단어_순서가_달라도_모든_토큰이_걸리면_찾는다")
    void search_multiToken_orderFree() {
        // given
        Long kim = saveArtist("김광석");
        saveProduct(kim, "KS 001", "김광석 다시 부르기");

        // when
        ProductSearchPage result = search("부르기 김광석", 0, 20);

        // then
        assertThat(result.content()).extracting(ProductSearchHit::title).containsExactly("김광석 다시 부르기");
    }

    @Test
    @DisplayName("토큰이_제목과_아티스트명에_나뉘어_걸려도_찾는다")
    void search_multiToken_acrossFields() {
        // given — "비틀즈"는 아티스트 별칭, "abbey"는 제목에만 있다
        Long beatles = saveArtist("The Beatles", "비틀즈");
        saveProduct(beatles, "PCS 7088", "Abbey Road");

        // when
        ProductSearchPage result = search("비틀즈 abbey", 0, 20);

        // then
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("카탈로그_번호_앞부분_일치가_텍스트_일치보다_먼저_나온다")
    void search_catalogArm_ranksFirst() {
        // given — B(제목 매칭)가 A(카탈로그 매칭)보다 먼저 저장돼 id가 작다
        Long beatles = saveArtist("The Beatles");
        Product textMatch = saveProduct(beatles, "PCS 7088", "CL Best 1355");
        Product catalogMatch = saveProduct(beatles, "CL 1355", "Some Album");

        // when
        ProductSearchPage result = search("CL 1355", 0, 20);

        // then — 카탈로그 일치가 1번째, 중복 없음
        assertThat(result.content()).extracting(ProductSearchHit::productId)
                .containsExactly(catalogMatch.getId(), textMatch.getId());
        assertThat(result.totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("제목_정확_일치가_부분_일치보다_먼저_나온다")
    void search_exactTitle_ranksAboveContains() {
        // given — 부분 일치 쪽이 먼저 저장돼 id가 작다
        Long beatles = saveArtist("The Beatles");
        saveProduct(beatles, "B1", "Let It Be Naked");
        saveProduct(beatles, "B2", "Let It Be");

        // when
        ProductSearchPage result = search("let it be", 0, 20);

        // then
        assertThat(result.content()).extracting(ProductSearchHit::title)
                .containsExactly("Let It Be", "Let It Be Naked");
    }

    @Test
    @DisplayName("전체_건수는_페이지와_무관하게_같다")
    void search_totalCount_consistentAcrossPages() {
        // given
        Long beatles = saveArtist("The Beatles");
        saveProduct(beatles, "A1", "Abbey Road");
        saveProduct(beatles, "A2", "Abbey Road");
        saveProduct(beatles, "A3", "Abbey Road");

        // when
        ProductSearchPage firstPage = search("abbeyroad", 0, 2);
        ProductSearchPage secondPage = search("abbeyroad", 1, 2);

        // then
        assertThat(firstPage.totalElements()).isEqualTo(3L);
        assertThat(secondPage.totalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("숫자_없는_검색어는_카탈로그_번호와_우연히_겹쳐도_그걸로_찾지_않는다")
    void search_noDigitKeyword_skipsCatalogArm() {
        // given — 카탈로그 번호만 "jazz"로 시작하고 제목·아티스트는 무관한 상품
        Long miles = saveArtist("Miles Davis");
        saveProduct(miles, "JAZZ 100", "Blue Train");
        saveProduct(miles, "BT 200", "Jazz Hits");

        // when
        ProductSearchPage result = search("jazz", 0, 20);

        // then — 제목이 걸린 상품만 나온다
        assertThat(result.content()).extracting(ProductSearchHit::title).containsExactly("Jazz Hits");
    }

    @Test
    @DisplayName("제목과_카탈로그가_둘_다_걸리는_상품은_숫자_없는_검색어에서도_나온다")
    void search_noDigitKeyword_bothMatchProductStillFound() {
        // given — 카탈로그 갈래가 꺼질 때 제외 조건만 남으면 이 상품이 결과에서 사라지는 회귀를 잡는다
        Long miles = saveArtist("Miles Davis");
        saveProduct(miles, "JAZZ 100", "Jazz Hits");

        // when
        ProductSearchPage result = search("jazz", 0, 20);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("범위_밖_페이지는_빈_내용이어도_전체_건수를_돌려준다")
    void search_pageBeyondRange_stillReturnsTotal() {
        // given
        Long beatles = saveArtist("The Beatles");
        saveProduct(beatles, "A1", "Abbey Road");

        // when — 매칭 1건뿐인데 5페이지를 요청
        ProductSearchPage result = search("abbeyroad", 5, 20);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(1L);
    }
}
