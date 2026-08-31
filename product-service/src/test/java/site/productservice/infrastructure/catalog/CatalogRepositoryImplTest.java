package site.productservice.infrastructure.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.productservice.domain.Artist;
import site.productservice.domain.PressType;
import site.productservice.domain.Product;
import site.productservice.domain.catalog.CatalogItem;
import site.productservice.domain.catalog.CatalogPage;
import site.productservice.domain.catalog.CatalogRepository;
import site.productservice.infrastructure.ArtistJpaRepository;
import site.productservice.infrastructure.ProductJpaRepository;
import site.productservice.support.RepositoryTest;

@RepositoryTest
@Import({CatalogRepositoryImpl.class, ActiveProductCounter.class})
class CatalogRepositoryImplTest {

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private ArtistJpaRepository artistJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    private Long saveArtist() {
        return artistJpaRepository.save(Artist.of("The Beatles")).getId();
    }

    private void saveProducts(Long artistId, int count) {
        for (int i = 0; i < count; i++) {
            productJpaRepository.save(Product.of("CAT-" + i, artistId, "Title " + i, "UK", 1969,
                    PressType.ORIGINAL, "LP", "Apple Records", "Rock", null, null));
        }
    }

    @Test
    @DisplayName("한 페이지보다 한 건이라도 더 있으면 요청한 만큼만 담고 hasNext가 참이다")
    void findActivePage_다음이_있으면_잘라내고_hasNext_참() {
        // given
        saveProducts(saveArtist(), 21);

        // when
        CatalogPage page = catalogRepository.findActivePage(0, 20);

        // then
        assertThat(page.items()).hasSize(20);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.totalElements()).isEqualTo(21L);
    }

    @Test
    @DisplayName("정확히 한 페이지만큼이면 hasNext가 거짓이다")
    void findActivePage_정확히_한_페이지면_hasNext_거짓() {
        // given
        saveProducts(saveArtist(), 20);

        // when
        CatalogPage page = catalogRepository.findActivePage(0, 20);

        // then
        assertThat(page.items()).hasSize(20);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("한 페이지에 못 미치면 있는 만큼 담고 hasNext가 거짓이다")
    void findActivePage_한_페이지_미만() {
        // given
        saveProducts(saveArtist(), 17);

        // when
        CatalogPage page = catalogRepository.findActivePage(0, 20);

        // then
        assertThat(page.items()).hasSize(17);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("두 번째 페이지는 첫 페이지 다음 상품부터 시작한다")
    void findActivePage_두번째_페이지_시작점() {
        // given
        saveProducts(saveArtist(), 21);
        CatalogPage first = catalogRepository.findActivePage(0, 20);

        // when
        CatalogPage second = catalogRepository.findActivePage(1, 20);

        // then
        assertThat(second.items()).hasSize(1);
        assertThat(second.hasNext()).isFalse();
        assertThat(second.items().get(0).productId())
                .isNotIn(first.items().stream().map(CatalogItem::productId).toList());
    }

    @Test
    @DisplayName("범위를 넘은 페이지는 빈 목록이지만 전체 건수는 그대로 돌려준다")
    void findActivePage_범위_밖_페이지() {
        // given
        saveProducts(saveArtist(), 21);

        // when
        CatalogPage page = catalogRepository.findActivePage(5, 20);

        // then
        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.totalElements()).isEqualTo(21L);
    }

    @Test
    @DisplayName("시작 위치가 셀 수 있는 범위를 넘는 페이지도 빈 목록으로 답한다")
    void findActivePage_시작위치_범위_초과() {
        // given
        saveProducts(saveArtist(), 21);

        // when
        CatalogPage page = catalogRepository.findActivePage(Integer.MAX_VALUE, 20);

        // then
        // 예외를 던지면 500으로 나간다. 상품이 없는 자리라는 점은 위 범위 밖 페이지와 다르지 않다
        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.totalElements()).isEqualTo(21L);
    }

    @Test
    @DisplayName("최신 등록순으로 내려준다")
    void findActivePage_최신순() {
        // given
        saveProducts(saveArtist(), 3);

        // when
        CatalogPage page = catalogRepository.findActivePage(0, 20);

        // then
        assertThat(page.items()).extracting(CatalogItem::productId)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }
}
