package site.productservice.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.productservice.domain.price.MediaCondition;
import site.productservice.domain.PressType;
import site.productservice.domain.price.PriceHistory;
import site.productservice.domain.price.PriceHistoryRepository;
import site.productservice.domain.Product;
import site.productservice.domain.ProductRepository;

@ExtendWith(MockitoExtension.class)
class PriceHistorySeedLoaderTest {

    // 대상 수는 원장에서 계산한다 — 기준가를 더하거나 빼도 테스트를 고칠 필요가 없도록
    private static final int TARGET_COUNT = (int) ProductSeedData.PRODUCTS.stream()
            .filter(product -> product.basePrice() != null)
            .count();
    private static final int TRADES_PER_PRODUCT = MediaCondition.values().length
            * PriceHistorySeedLoader.TRADES_PER_CONDITION;

    @Mock private ProductRepository productRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;
    @InjectMocks private PriceHistorySeedLoader seedLoader;

    private Product productWithId(long id) {
        Product product = Product.of("PCS 7088", 1L, "Abbey Road", "UK", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    /** 대상 상품 조회가 호출될 때마다 서로 다른 id의 상품을 돌려준다 (1, 2, 3, ...). */
    private void givenEveryTargetProductExists() {
        AtomicLong nextId = new AtomicLong(1);
        given(productRepository.findByNaturalKey(any(), any(), any()))
                .willAnswer(inv -> Optional.of(productWithId(nextId.getAndIncrement())));
    }

    @Test
    @DisplayName("빈 DB면 기준가 있는 상품마다 36건씩, 경매 id가 전부 다르게 저장된다")
    void run_빈_DB면_대상_상품마다_36건_저장() {
        // given
        givenEveryTargetProductExists();
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(false);

        // when
        seedLoader.run();

        // then
        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        then(priceHistoryRepository).should(times(TARGET_COUNT * TRADES_PER_PRODUCT)).save(captor.capture());
        List<PriceHistory> saved = captor.getAllValues();
        assertThat(saved).extracting(PriceHistory::getAuctionId).doesNotHaveDuplicates();
        // 상품당 건수는 시세 조회 상한(PriceQueryService.RECENT_TRADES_LIMIT = 100)보다 작아야
        // "최근 100건" 잘림 없이 전부 화면에 나온다
        assertThat(TRADES_PER_PRODUCT).isLessThan(100);
        for (long productId = 1; productId <= TARGET_COUNT; productId++) {
            long currentProductId = productId;
            assertThat(saved.stream().filter(trade -> trade.getProductId() == currentProductId))
                    .hasSize(TRADES_PER_PRODUCT);
        }
    }

    @Test
    @DisplayName("거래 시각은 미래가 아니고, 확정 시각은 거래 시각 이후다")
    void run_시각_질서_보장() {
        // given
        givenEveryTargetProductExists();
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(false);

        // when
        LocalDateTime before = LocalDateTime.now();
        seedLoader.run();

        // then
        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        then(priceHistoryRepository).should(times(TARGET_COUNT * TRADES_PER_PRODUCT)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(trade -> {
            assertThat(trade.getTradedAt()).isBefore(before);
            assertThat(trade.getConfirmedAt()).isAfter(trade.getTradedAt());
        });
    }

    @Test
    @DisplayName("전부 이미 있으면 아무것도 저장하지 않는다 (여러 번 실행해도 안전)")
    void run_전부_있으면_저장_없음() {
        // given
        givenEveryTargetProductExists();
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(true);

        // when
        seedLoader.run();

        // then
        then(priceHistoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("대상 상품이 DB에 없으면 그 상품 것만 건너뛰고 나머지는 저장한다")
    void run_상품_없으면_그_상품만_건너뜀() {
        // given — 첫 번째 대상만 없음
        AtomicInteger callCount = new AtomicInteger();
        AtomicLong nextId = new AtomicLong(1);
        given(productRepository.findByNaturalKey(any(), any(), any())).willAnswer(inv ->
                callCount.getAndIncrement() == 0
                        ? Optional.empty()
                        : Optional.of(productWithId(nextId.getAndIncrement())));
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(false);

        // when
        seedLoader.run();

        // then
        then(priceHistoryRepository).should(times((TARGET_COUNT - 1) * TRADES_PER_PRODUCT)).save(any());
    }

    @Test
    @DisplayName("컨디션 6등급에 등급별 6건씩 분산되고, 좋은 등급이 나쁜 등급보다 항상 비싸다")
    void run_컨디션_분산과_가격_질서() {
        // given
        givenEveryTargetProductExists();
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(false);

        // when
        seedLoader.run();

        // then — 첫 번째 상품(id 1)만 떼어 확인
        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        then(priceHistoryRepository).should(times(TARGET_COUNT * TRADES_PER_PRODUCT)).save(captor.capture());
        List<PriceHistory> ofProduct = captor.getAllValues().stream()
                .filter(trade -> trade.getProductId() == 1L)
                .toList();
        for (MediaCondition condition : MediaCondition.values()) {
            List<PriceHistory> ofCondition = ofProduct.stream()
                    .filter(trade -> trade.getMediaCondition() == condition)
                    .toList();
            assertThat(ofCondition).hasSize(PriceHistorySeedLoader.TRADES_PER_CONDITION);
        }
        long mintLowest = ofProduct.stream()
                .filter(trade -> trade.getMediaCondition() == MediaCondition.MINT)
                .mapToLong(PriceHistory::getFinalPrice)
                .min()
                .orElseThrow();
        long poorHighest = ofProduct.stream()
                .filter(trade -> trade.getMediaCondition() == MediaCondition.POOR)
                .mapToLong(PriceHistory::getFinalPrice)
                .max()
                .orElseThrow();
        assertThat(mintLowest).isGreaterThan(poorHighest);
    }
}
