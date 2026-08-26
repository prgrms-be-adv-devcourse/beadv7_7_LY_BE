package site.productservice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.productservice.application.dto.ProductDetailResult;
import site.productservice.application.dto.ProductListQuery;
import site.productservice.application.dto.ProductListResult;
import site.productservice.application.dto.ProductPageResult;
import site.productservice.application.dto.ProductSnapshotResult;
import site.productservice.application.port.AuctionOpenCountPort;
import site.productservice.domain.Artist;
import site.productservice.domain.ArtistAliasRepository;
import site.productservice.domain.ArtistRepository;
import site.productservice.domain.PressType;
import site.productservice.domain.price.PriceHistory;
import site.productservice.domain.price.PriceHistoryRepository;
import site.productservice.domain.Product;
import site.productservice.domain.ProductAliasRepository;
import site.productservice.domain.search.ProductSearchHit;
import site.productservice.domain.search.ProductSearchPage;
import site.productservice.domain.search.ProductSearchRepository;
import site.productservice.exception.ProductNotFoundException;
import site.productservice.domain.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final ProductSearchHit CATALOG_HIT =
            new ProductSearchHit(55L, "Abbey Road", "The Beatles", null, 1969, PressType.ORIGINAL, "UK");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private AuctionOpenCountPort auctionOpenCountPort;

    @Mock
    private ProductAliasRepository productAliasRepository;

    @Mock
    private ArtistAliasRepository artistAliasRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Artist artist;

    @BeforeEach
    void setUp() {
        artist = Artist.of("The Beatles");
        ReflectionTestUtils.setField(artist, "id", 3L);
        product = Product.of("PCS 7088", 3L, "Abbey Road", "UK", 1969,
                PressType.ORIGINAL, "LP", "Apple Records", "Rock", null, "1969년 영국 오리지널 프레싱");
        ReflectionTestUtils.setField(product, "id", 55L);
    }

    @Test
    @DisplayName("상세 조회는 상품과 아티스트를 합성해 반환한다")
    void getActiveProductDetail_상품과_아티스트를_합성해_반환() {
        // given
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));

        // when
        ProductDetailResult result = productService.getActiveProductDetail(55L);

        // then
        assertThat(result.productId()).isEqualTo(55L);
        assertThat(result.catalogNumber()).isEqualTo("PCS 7088");
        assertThat(result.title()).isEqualTo("Abbey Road");
        assertThat(result.artist().artistId()).isEqualTo(3L);
        assertThat(result.artist().name()).isEqualTo("The Beatles");
        assertThat(result.label()).isEqualTo("Apple Records");
        assertThat(result.country()).isEqualTo("UK");
        assertThat(result.releaseYear()).isEqualTo(1969);
        assertThat(result.pressType()).isEqualTo(PressType.ORIGINAL);
        assertThat(result.format()).isEqualTo("LP");
        assertThat(result.genre()).isEqualTo("Rock");
        assertThat(result.coverImageUrl()).isNull();
        assertThat(result.description()).isEqualTo("1969년 영국 오리지널 프레싱");
    }

    @Test
    @DisplayName("상세 조회는 없는 상품이면 상품없음 예외를 던진다")
    void getActiveProductDetail_없는_상품이면_예외() {
        // given
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getActiveProductDetail(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("상세 조회는 비활성 상품이면 상품없음 예외를 던진다 (사용자에겐 없는 상품)")
    void getActiveProductDetail_비활성_상품이면_예외() {
        // given
        product.deactivate();
        given(productRepository.findById(55L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.getActiveProductDetail(55L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("내부 조회는 비활성 상품도 active 플래그와 함께 반환한다 (검증 소스)")
    void getProductSnapshot_비활성_상품도_반환() {
        // given
        product.deactivate();
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));

        // when
        ProductSnapshotResult result = productService.getProductSnapshot(55L);

        // then
        assertThat(result.productId()).isEqualTo(55L);
        assertThat(result.active()).isFalse();
        assertThat(result.artistName()).isEqualTo("The Beatles");
        assertThat(result.mergedIntoId()).isNull();
    }

    @Test
    @DisplayName("내부 조회는 없는 상품이면 상품없음 예외를 던진다")
    void getProductSnapshot_없는_상품이면_예외() {
        // given
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProductSnapshot(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("상품이 참조하는 아티스트가 없으면 정합성 예외를 던진다")
    void getActiveProductDetail_아티스트_부재면_정합성_예외() {
        // given
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(artistRepository.findById(3L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getActiveProductDetail(55L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("배치 조회는 여러 상품과 아티스트를 합성해 반환한다")
    void getProductSnapshots_여러_상품과_아티스트를_합성해_반환() {
        // given
        Product anotherProduct = Product.of("PCS 7089", 3L, "Let It Be", "UK", 1970,
                PressType.ORIGINAL, "LP", "Apple Records", "Rock", null, "1970년 영국 오리지널 프레싱");
        ReflectionTestUtils.setField(anotherProduct, "id", 56L);

        given(productRepository.findAllByIds(List.of(55L, 56L))).willReturn(List.of(product, anotherProduct));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of(artist));

        // when
        List<ProductSnapshotResult> results = productService.getProductSnapshots(List.of(55L, 56L));

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ProductSnapshotResult::productId).containsExactlyInAnyOrder(55L, 56L);
        assertThat(results).allMatch(r -> r.artistName().equals("The Beatles"));
    }

    @Test
    @DisplayName("배치 조회는 요청한 id 중 일부가 없으면 존재하는 상품만 반환한다")
    void getProductSnapshots_없는_상품이_있으면_존재하는_것만_반환() {
        // given
        given(productRepository.findAllByIds(List.of(55L, 99L))).willReturn(List.of(product));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of(artist));

        // when
        List<ProductSnapshotResult> results = productService.getProductSnapshots(List.of(55L, 99L));

        // then
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().productId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("배치 조회는 상품이 참조하는 아티스트가 없으면 정합성 예외를 던진다")
    void getProductSnapshots_아티스트_부재면_정합성_예외() {
        // given
        given(productRepository.findAllByIds(List.of(55L))).willReturn(List.of(product));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> productService.getProductSnapshots(List.of(55L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("전체 순회는 다음 페이지 존재를 알아내려고 size보다 한 건 더 조회한다")
    void getProductPage는_size보다_한건_더_조회한다() {
        given(productRepository.findAllOrderByIdAfter(null, 101)).willReturn(List.of());

        productService.getProductPage(null, 100);

        then(productRepository).should().findAllOrderByIdAfter(null, 101);
    }

    @Test
    @DisplayName("전체 순회는 cursor를 그대로 리포지토리에 전달한다")
    void getProductPage는_cursor를_그대로_전달한다() {
        given(productRepository.findAllOrderByIdAfter(50L, 101)).willReturn(List.of());

        productService.getProductPage(50L, 100);

        then(productRepository).should().findAllOrderByIdAfter(50L, 101);
    }

    @Test
    @DisplayName("전체 순회는 size보다 많이 조회되면 잘라내고 hasNext와 nextCursor를 설정한다")
    void getProductPage는_넘치면_hasNext와_nextCursor를_설정한다() {
        Product first = productWithId(55L);
        Product second = productWithId(56L);
        Product third = productWithId(57L);
        given(productRepository.findAllOrderByIdAfter(null, 3))
                .willReturn(List.of(first, second, third));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of(artist));

        ProductPageResult page = productService.getProductPage(null, 2);

        assertThat(page.items()).extracting(ProductSnapshotResult::productId).containsExactly(55L, 56L);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(56L);
    }

    @Test
    @DisplayName("전체 순회는 size 이하로 조회되면 hasNext가 false이고 nextCursor가 없다")
    void getProductPage는_넘치지_않으면_다음_페이지가_없다() {
        given(productRepository.findAllOrderByIdAfter(null, 101)).willReturn(List.of(product));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of(artist));

        ProductPageResult page = productService.getProductPage(null, 100);

        assertThat(page.items()).extracting(ProductSnapshotResult::productId).containsExactly(55L);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("전체 순회는 size가 0 이하면 기본값 100으로 조회한다")
    void getProductPage는_size가_0이하면_기본값을_쓴다() {
        given(productRepository.findAllOrderByIdAfter(null, 101)).willReturn(List.of());

        productService.getProductPage(null, 0);

        then(productRepository).should().findAllOrderByIdAfter(null, 101);
    }

    @Test
    @DisplayName("전체 순회는 size가 최대치를 넘으면 500으로 제한한다")
    void getProductPage는_size가_최대치를_넘으면_제한한다() {
        given(productRepository.findAllOrderByIdAfter(null, 501)).willReturn(List.of());

        productService.getProductPage(null, 5000);

        then(productRepository).should().findAllOrderByIdAfter(null, 501);
    }

    @Test
    @DisplayName("전체 순회 결과도 아티스트를 합성해 반환한다")
    void getProductPage는_아티스트를_합성해_반환한다() {
        given(productRepository.findAllOrderByIdAfter(null, 101)).willReturn(List.of(product));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of(artist));

        ProductPageResult page = productService.getProductPage(null, 100);

        assertThat(page.items().getFirst().artistName()).isEqualTo("The Beatles");
    }

    @Test
    @DisplayName("전체 순회는 결과가 없으면 빈 페이지를 반환한다")
    void getProductPage는_결과가_없으면_빈_페이지() {
        given(productRepository.findAllOrderByIdAfter(null, 101)).willReturn(List.of());

        ProductPageResult page = productService.getProductPage(null, 100);

        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("전체 순회는 상품과 아티스트의 별칭을 함께 싣는다")
    void getProductPage는_별칭을_싣는다() {
        // given
        Product product = productWithId(55L);
        given(productRepository.findAllOrderByIdAfter(null, 101)).willReturn(List.of(product));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of(artist));
        given(productAliasRepository.findNamesByProductIds(List.of(55L)))
                .willReturn(Map.of(55L, List.of("애비 로드")));
        given(artistAliasRepository.findNamesByArtistIds(List.of(3L)))
                .willReturn(Map.of(3L, List.of("비틀즈", "Beatles")));

        // when
        ProductPageResult page = productService.getProductPage(null, 100);

        // then
        assertThat(page.items().getFirst().titleAliases()).containsExactly("애비 로드");
        assertThat(page.items().getFirst().artistAliases())
                .containsExactlyInAnyOrder("비틀즈", "Beatles");
    }

    @Test
    @DisplayName("별칭이 없는 상품은 빈 목록을 싣는다")
    void getProductPage는_별칭이_없으면_빈_목록() {
        // given
        // null이 그대로 흘러가면 색인 문서의 필드가 비고, 검색이 안 될 때 원인을 찾기 어렵다
        Product product = productWithId(55L);
        given(productRepository.findAllOrderByIdAfter(null, 101)).willReturn(List.of(product));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of(artist));
        given(productAliasRepository.findNamesByProductIds(List.of(55L))).willReturn(Map.of());
        given(artistAliasRepository.findNamesByArtistIds(List.of(3L))).willReturn(Map.of());

        // when
        ProductPageResult page = productService.getProductPage(null, 100);

        // then
        assertThat(page.items().getFirst().titleAliases()).isEmpty();
        assertThat(page.items().getFirst().artistAliases()).isEmpty();
    }

    @Test
    @DisplayName("같은 아티스트의 상품 여럿에 아티스트 별칭이 각각 실린다")
    void getProductPage는_아티스트_별칭을_상품마다_배분한다() {
        // given
        // 별칭은 아티스트에 붙어 있어 조회는 아티스트 단위지만, 담기는 것은 상품 단위다.
        // 상품 id로 조회했다고 착각하면 여기서 드러난다.
        Product first = productWithId(55L);
        Product second = productWithId(56L);
        given(productRepository.findAllOrderByIdAfter(null, 101))
                .willReturn(List.of(first, second));
        given(artistRepository.findAllByIds(List.of(3L))).willReturn(List.of(artist));
        given(productAliasRepository.findNamesByProductIds(List.of(55L, 56L))).willReturn(Map.of());
        given(artistAliasRepository.findNamesByArtistIds(List.of(3L)))
                .willReturn(Map.of(3L, List.of("비틀즈")));

        // when
        ProductPageResult page = productService.getProductPage(null, 100);

        // then
        assertThat(page.items()).allSatisfy(item ->
                assertThat(item.artistAliases()).containsExactly("비틀즈"));
    }

    @Test
    @DisplayName("단건 스냅샷은 별칭을 채우지 않는다")
    void getProductSnapshot은_별칭이_비어_있다() {
        // given
        // 경매·주문·위시리스트가 쓰는 경로다. 별칭을 쓰는 곳이 없어 조회만 늘어난다.
        Product product = productWithId(55L);
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));

        // when
        ProductSnapshotResult result = productService.getProductSnapshot(55L);

        // then
        assertThat(result.titleAliases()).isEmpty();
        assertThat(result.artistAliases()).isEmpty();
    }

    private Product productWithId(Long id) {
        Product p = Product.of("CAT-" + id, 3L, "Title " + id, "UK", 1969,
                PressType.ORIGINAL, "LP", "Apple Records", "Rock", null, null);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    @DisplayName("목록 조회는 상품 카드에 발매국과 진행 중 경매 수를 함께 담는다")
    void getProductList_카드_필드_조합() {
        // given
        given(productSearchRepository.findActivePage(0, 20))
                .willReturn(new ProductSearchPage(List.of(CATALOG_HIT), 1L));
        given(priceHistoryRepository.findLatestTrades(List.of(55L))).willReturn(List.of());
        given(auctionOpenCountPort.findOpenAuctionCounts(List.of(55L))).willReturn(Map.of(55L, 2L));

        // when
        ProductListResult result = productService.getProductList(new ProductListQuery(0, 20));

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).country()).isEqualTo("UK");
        assertThat(result.content().get(0).openAuctionCount()).isEqualTo(2L);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("거래 이력이 있는 상품은 최근 낙찰가를, 없는 상품은 null을 담는다")
    void getProductList_최근_낙찰가_병합() {
        // given
        ProductSearchHit second = new ProductSearchHit(56L, "Kind of Blue", "Miles Davis", null, 1959,
                PressType.REISSUE, "US");
        given(productSearchRepository.findActivePage(0, 20))
                .willReturn(new ProductSearchPage(List.of(CATALOG_HIT, second), 2L));
        PriceHistory trade = mock(PriceHistory.class);
        given(trade.getProductId()).willReturn(55L);
        given(trade.getFinalPrice()).willReturn(132000L);
        given(priceHistoryRepository.findLatestTrades(List.of(55L, 56L))).willReturn(List.of(trade));
        given(auctionOpenCountPort.findOpenAuctionCounts(List.of(55L, 56L))).willReturn(Map.of());

        // when
        ProductListResult result = productService.getProductList(new ProductListQuery(0, 20));

        // then
        assertThat(result.content().get(0).lastTradedPrice()).isEqualTo(132000L);
        assertThat(result.content().get(1).lastTradedPrice()).isNull();
    }

    @Test
    @DisplayName("경매 건수 조회가 실패해도 목록은 정상 반환하고 openAuctionCount만 전부 null이 된다")
    void getProductList_경매_조회_실패시_건수만_null() {
        // given
        given(productSearchRepository.findActivePage(0, 20))
                .willReturn(new ProductSearchPage(List.of(CATALOG_HIT), 1L));
        given(priceHistoryRepository.findLatestTrades(List.of(55L))).willReturn(List.of());
        given(auctionOpenCountPort.findOpenAuctionCounts(List.of(55L)))
                .willThrow(new IllegalStateException("경매 서비스 응답 없음"));

        // when
        ProductListResult result = productService.getProductList(new ProductListQuery(0, 20));

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).openAuctionCount()).isNull();
    }

    @Test
    @DisplayName("경매 건수 응답에 없는 상품은 0건으로 담는다")
    void getProductList_건수_응답에_없으면_0() {
        // given
        given(productSearchRepository.findActivePage(0, 20))
                .willReturn(new ProductSearchPage(List.of(CATALOG_HIT), 1L));
        given(priceHistoryRepository.findLatestTrades(List.of(55L))).willReturn(List.of());
        given(auctionOpenCountPort.findOpenAuctionCounts(List.of(55L))).willReturn(Map.of());

        // when
        ProductListResult result = productService.getProductList(new ProductListQuery(0, 20));

        // then
        assertThat(result.content().get(0).openAuctionCount()).isZero();
    }

    @Test
    @DisplayName("결과가 빈 페이지면 시세·경매 조회를 하지 않는다")
    void getProductList_빈_페이지면_추가_조회_생략() {
        // given
        given(productSearchRepository.findActivePage(0, 20))
                .willReturn(new ProductSearchPage(List.of(), 0L));

        // when
        productService.getProductList(new ProductListQuery(0, 20));

        // then
        then(priceHistoryRepository).shouldHaveNoInteractions();
        then(auctionOpenCountPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("size는 1 미만이면 기본값 20, 100 초과면 100으로, page는 음수면 0으로 보정한다")
    void getProductList_페이징_보정() {
        // given
        given(productSearchRepository.findActivePage(0, 20))
                .willReturn(new ProductSearchPage(List.of(), 0L));
        given(productSearchRepository.findActivePage(0, 100))
                .willReturn(new ProductSearchPage(List.of(), 0L));

        // when
        productService.getProductList(new ProductListQuery(-1, 0));
        productService.getProductList(new ProductListQuery(0, 101));

        // then
        then(productSearchRepository).should().findActivePage(0, 20);
        then(productSearchRepository).should().findActivePage(0, 100);
    }
}
