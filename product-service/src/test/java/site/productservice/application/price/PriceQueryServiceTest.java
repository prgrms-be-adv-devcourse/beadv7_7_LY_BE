package site.productservice.application.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.productservice.application.dto.price.PriceSummaryResult;
import site.productservice.application.dto.price.PriceTradesResult;
import site.productservice.application.dto.price.RecentTradesResult;
import site.productservice.domain.Artist;
import site.productservice.domain.ArtistRepository;
import site.productservice.domain.price.ClosedAuction;
import site.productservice.domain.price.MediaCondition;
import site.productservice.domain.PressType;
import site.productservice.domain.price.PriceHistory;
import site.productservice.domain.price.PriceHistoryRepository;
import site.productservice.domain.Product;
import site.productservice.domain.ProductRepository;
import site.productservice.exception.ProductNotFoundException;

@ExtendWith(MockitoExtension.class)
class PriceQueryServiceTest {

    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 7, 10, 20, 31);
    private static final LocalDateTime CONFIRMED_AT = LocalDateTime.of(2026, 7, 11, 10, 0);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private PriceQueryService priceQueryService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.of("PCS 7088", 3L, "Abbey Road", "UK", 1969,
                PressType.ORIGINAL, "LP", "Apple Records", "Rock", null, "1969년 영국 오리지널 프레싱");
    }

    private PriceHistory trade(long auctionId, MediaCondition condition, long finalPrice) {
        return PriceHistory.of(
                new ClosedAuction(auctionId, 55L, condition, finalPrice, 5, CLOSED_AT, "ENDED_WON"),
                CONFIRMED_AT);
    }

    @Test
    @DisplayName("요약은 최근 100건 창을 읽어 컨디션별 통계를 반환한다")
    void getPriceSummary_최근_100건_창으로_집계() {
        // given — limit 100이 아니면 스텁이 응답하지 않아 실패한다 (창 크기 계약 고정)
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(priceHistoryRepository.findRecentTrades(55L, 100)).willReturn(List.of(
                trade(1L, MediaCondition.NEAR_MINT, 80000L),
                trade(2L, MediaCondition.NEAR_MINT, 90000L)));

        // when
        PriceSummaryResult result = priceQueryService.getPriceSummary(55L);

        // then
        assertThat(result.productId()).isEqualTo(55L);
        assertThat(result.conditions()).hasSize(1);
        assertThat(result.conditions().get(0).condition()).isEqualTo(MediaCondition.NEAR_MINT);
        assertThat(result.conditions().get(0).sampleCount()).isEqualTo(2L);
        assertThat(result.conditions().get(0).averagePrice()).isEqualTo(85000L);
    }

    @Test
    @DisplayName("요약은 거래가 없으면 빈 목록을 반환한다 (예외 아님)")
    void getPriceSummary_거래_없으면_빈_목록() {
        // given
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(priceHistoryRepository.findRecentTrades(55L, 100)).willReturn(List.of());

        // when
        PriceSummaryResult result = priceQueryService.getPriceSummary(55L);

        // then
        assertThat(result.conditions()).isEmpty();
    }

    @Test
    @DisplayName("요약은 없는 상품이면 상품없음 예외를 던진다")
    void getPriceSummary_없는_상품이면_예외() {
        // given
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> priceQueryService.getPriceSummary(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("요약은 비활성 상품이면 상품없음 예외를 던진다 (사용자에겐 없는 상품)")
    void getPriceSummary_비활성_상품이면_예외() {
        // given
        product.deactivate();
        given(productRepository.findById(55L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> priceQueryService.getPriceSummary(55L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("추이는 요약과 같은 최근 100건 창을 최신순 그대로 반환한다")
    void getPriceTrades_같은_창을_그대로_반환() {
        // given
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(priceHistoryRepository.findRecentTrades(55L, 100)).willReturn(List.of(
                trade(2L, MediaCondition.NEAR_MINT, 90000L),
                trade(1L, MediaCondition.GOOD, 20000L)));

        // when
        PriceTradesResult result = priceQueryService.getPriceTrades(55L);

        // then — 리포지토리가 준 순서(최신순)를 재정렬 없이 유지
        assertThat(result.trades()).hasSize(2);
        assertThat(result.trades().get(0).price()).isEqualTo(90000L);
        assertThat(result.trades().get(1).price()).isEqualTo(20000L);
    }

    @Test
    @DisplayName("추이는 없는 상품이면 상품없음 예외를 던진다")
    void getPriceTrades_없는_상품이면_예외() {
        // given
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> priceQueryService.getPriceTrades(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("추이는 비활성 상품이면 상품없음 예외를 던진다")
    void getPriceTrades_비활성_상품이면_예외() {
        // given
        product.deactivate();
        given(productRepository.findById(55L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> priceQueryService.getPriceTrades(55L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("전역 최근 낙찰은 상품·아티스트 정보를 조합해 최신순 그대로 반환한다")
    void getRecentTrades_상품_아티스트_조합() {
        // given
        Product first = productWithId(55L, 3L, "Abbey Road");
        Product second = productWithId(56L, 4L, "Kind Of Blue");
        given(priceHistoryRepository.findRecent(4)).willReturn(List.of(
                tradeOf(1L, 55L, 70_000), tradeOf(2L, 56L, 45_000)));
        given(productRepository.findAllByIds(List.of(55L, 56L))).willReturn(List.of(first, second));
        given(artistRepository.findAllByIds(anyList()))
                .willReturn(List.of(artistWithId(3L, "The Beatles"), artistWithId(4L, "Miles Davis")));

        // when
        RecentTradesResult result = priceQueryService.getRecentTrades(2);

        // then
        assertThat(result.trades()).hasSize(2);
        assertThat(result.trades().get(0).productId()).isEqualTo(55L);
        assertThat(result.trades().get(0).artistName()).isEqualTo("The Beatles");
        assertThat(result.trades().get(0).price()).isEqualTo(70_000);
        assertThat(result.trades().get(1).title()).isEqualTo("Kind Of Blue");
    }

    @Test
    @DisplayName("표시할 상품이 없는 거래(비활성·삭제)는 건너뛴다")
    void getRecentTrades_상품_없는_거래_제외() {
        // given — 55는 활성, 56은 비활성, 57은 조회 결과에 없음(삭제)
        Product active = productWithId(55L, 3L, "Abbey Road");
        Product inactive = productWithId(56L, 4L, "지워진 판");
        ReflectionTestUtils.setField(inactive, "active", false);
        given(priceHistoryRepository.findRecent(4)).willReturn(List.of(
                tradeOf(1L, 56L, 90_000), tradeOf(2L, 57L, 80_000), tradeOf(3L, 55L, 70_000)));
        given(productRepository.findAllByIds(List.of(56L, 57L, 55L))).willReturn(List.of(active, inactive));
        given(artistRepository.findAllByIds(anyList())).willReturn(List.of(artistWithId(3L, "The Beatles")));

        // when
        RecentTradesResult result = priceQueryService.getRecentTrades(2);

        // then
        assertThat(result.trades()).hasSize(1);
        assertThat(result.trades().get(0).productId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("요청 개수가 상한을 넘으면 20으로 자르고, 0 이하면 1로 올린다")
    void getRecentTrades_개수_경계() {
        // given — 여유분 2배를 읽는 계약까지 함께 고정한다
        given(priceHistoryRepository.findRecent(40)).willReturn(List.of());
        given(priceHistoryRepository.findRecent(2)).willReturn(List.of());

        // when
        priceQueryService.getRecentTrades(100);
        priceQueryService.getRecentTrades(0);

        // then
        then(priceHistoryRepository).should().findRecent(40);
        then(priceHistoryRepository).should().findRecent(2);
    }

    private Product productWithId(long id, long artistId, String title) {
        Product created = Product.of("CAT-" + id, artistId, title, "UK", 1996,
                PressType.ORIGINAL, "LP", "Label", "Rock", null, null);
        ReflectionTestUtils.setField(created, "id", id);
        return created;
    }

    private Artist artistWithId(long id, String name) {
        Artist created = Artist.of(name);
        ReflectionTestUtils.setField(created, "id", id);
        return created;
    }

    private PriceHistory tradeOf(long auctionId, long productId, long finalPrice) {
        return PriceHistory.of(
                new ClosedAuction(auctionId, productId, MediaCondition.NEAR_MINT, finalPrice, 5, CLOSED_AT,
                        "ENDED_WON"),
                CONFIRMED_AT);
    }
}
