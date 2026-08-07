package site.productservice.application.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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
import site.productservice.application.dto.price.PriceSummaryResult;
import site.productservice.application.dto.price.PriceTradesResult;
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
}
