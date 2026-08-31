package site.productservice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.productservice.domain.Artist;
import site.productservice.domain.PressType;
import site.productservice.domain.Product;
import site.productservice.domain.catalog.CatalogRepository;
import site.productservice.infrastructure.catalog.ActiveProductCounter;
import site.productservice.infrastructure.catalog.CatalogRepositoryImpl;
import site.productservice.support.RepositoryTest;

/**
 * 목록 화면의 전체 건수를 구하는 쿼리다. 같은 화면의 목록 쿼리와 세는 대상이 어긋나면 "전체 N건"과 실제로 넘길 수 있는
 * 페이지 수가 맞지 않게 되는데, 오류 없이 숫자만 틀리므로 여기서 잡는다.
 */
@RepositoryTest
@Import({CatalogRepositoryImpl.class, ActiveProductCounter.class})
@DisplayName("목록 전체 건수 조회")
class ProductCountActiveTest {

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ArtistJpaRepository artistJpaRepository;

    @Autowired
    private CatalogRepository catalogRepository;

    private Product saveProduct(Long artistId, String title) {
        return productJpaRepository.save(Product.of("CAT" + title, artistId, title, "UK", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null));
    }

    @Test
    @DisplayName("상품이 없으면 0을 준다")
    void 상품_없음() {
        // given
        // 저장한 상품이 없다

        // when
        long count = productJpaRepository.countActive();

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("판매를 내린 상품은 세지 않는다")
    void 비활성_제외() {
        // given
        Long artistId = artistJpaRepository.save(Artist.of("The Beatles")).getId();
        saveProduct(artistId, "Abbey Road");
        Product dropped = saveProduct(artistId, "Let It Be");
        dropped.deactivate();
        productJpaRepository.save(dropped);

        // when
        long count = productJpaRepository.countActive();

        // then
        assertThat(count).isEqualTo(1);
    }

    /**
     * 상품은 아티스트를 외래 키가 아니라 식별자 값으로만 참조한다. 그래서 존재하지 않는 아티스트를 가리키는 상품이
     * 데이터베이스 수준에서 막히지 않는다. 건수 쿼리가 아티스트를 조인하지 않기로 한 결정이 이 동작으로 드러난다 —
     * 조인을 되살리면 이 테스트가 깨진다.
     */
    @Test
    @DisplayName("가리키는 아티스트가 없어도 건수에는 들어간다")
    void 아티스트_없는_상품도_포함() {
        // given
        Long artistId = artistJpaRepository.save(Artist.of("The Beatles")).getId();
        saveProduct(artistId, "Abbey Road");
        saveProduct(artistId + 10_000L, "Unknown Artist Album");

        // when
        long count = productJpaRepository.countActive();

        // then
        assertThat(count).isEqualTo(2);
    }

    /**
     * 목록 쿼리(CatalogRepositoryImpl)는 아티스트 이름을 읽어야 해서 조인이 남아 있다. 위 상품이 목록에서는
     * 빠지므로, 두 숫자가 갈라진다는 사실 자체를 기록해 둔다. 지금 데이터에는 그런 상품이 없어 문제가 되지
     * 않지만, 생기면 여기가 근거다.
     */
    @Test
    @DisplayName("아티스트가 없는 상품은 목록에서는 빠져 건수와 어긋난다")
    void 목록과_건수가_갈라지는_경우() {
        // given
        Long artistId = artistJpaRepository.save(Artist.of("The Beatles")).getId();
        saveProduct(artistId, "Abbey Road");
        saveProduct(artistId + 10_000L, "Unknown Artist Album");

        // when
        long count = productJpaRepository.countActive();
        int listed = catalogRepository.findActivePage(0, 10).items().size();

        // then
        assertThat(count).isEqualTo(2);
        assertThat(listed).isEqualTo(1);
    }
}
