package site.coreservice.product.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.product.domain.MediaCondition;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.PriceHistory;
import site.coreservice.product.domain.PriceHistoryRepository;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.ProductRepository;
import site.coreservice.product.domain.TextNormalizer;

@ExtendWith(MockitoExtension.class)
class PriceHistorySeedLoaderTest {

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

    private void givenAllTargetProductsExist() {
        given(productRepository.findByNaturalKey("pcs7088", "LP", "UK"))
                .willReturn(Optional.of(productWithId(10L)));
        given(productRepository.findByNaturalKey("shvl804", "LP", "UK"))
                .willReturn(Optional.of(productWithId(11L)));
        given(productRepository.findByNaturalKey("cl1355", "LP", "US"))
                .willReturn(Optional.of(productWithId(12L)));
    }

    @Test
    @DisplayName("빈 DB면 대상 상품 3개에 각 36건씩, 경매 id가 전부 다르게 저장된다")
    void run_빈_DB면_대상_상품마다_36건_저장() {
        // given
        givenAllTargetProductsExist();
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(false);

        // when
        seedLoader.run();

        // then
        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        then(priceHistoryRepository).should(times(3 * TRADES_PER_PRODUCT)).save(captor.capture());
        List<PriceHistory> saved = captor.getAllValues();
        assertThat(saved).extracting(PriceHistory::getAuctionId).doesNotHaveDuplicates();
        for (long productId : new long[] {10L, 11L, 12L}) {
            List<PriceHistory> ofProduct = saved.stream()
                    .filter(trade -> trade.getProductId() == productId)
                    .toList();
            // 상품당 건수는 시세 조회 상한(PriceQueryService.RECENT_TRADES_LIMIT = 100)보다 작아야
            // "최근 100건" 잘림 없이 전부 화면에 나온다
            assertThat(ofProduct).hasSize(TRADES_PER_PRODUCT);
            assertThat(ofProduct.size()).isLessThan(100);
        }
    }

    @Test
    @DisplayName("거래 시각은 미래가 아니고, 확정 시각은 거래 시각 이후다")
    void run_시각_질서_보장() {
        // given
        givenAllTargetProductsExist();
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(false);

        // when
        LocalDateTime before = LocalDateTime.now();
        seedLoader.run();

        // then
        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        then(priceHistoryRepository).should(times(3 * TRADES_PER_PRODUCT)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(trade -> {
            assertThat(trade.getTradedAt()).isBefore(before);
            assertThat(trade.getConfirmedAt()).isAfter(trade.getTradedAt());
        });
    }

    @Test
    @DisplayName("전부 이미 있으면 아무것도 저장하지 않는다 (여러 번 실행해도 안전)")
    void run_전부_있으면_저장_없음() {
        // given
        givenAllTargetProductsExist();
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(true);

        // when
        seedLoader.run();

        // then
        then(priceHistoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("대상 상품이 DB에 없으면 그 상품 것만 건너뛰고 나머지는 저장한다")
    void run_상품_없으면_그_상품만_건너뜀() {
        // given — Abbey Road만 없음
        given(productRepository.findByNaturalKey("pcs7088", "LP", "UK")).willReturn(Optional.empty());
        given(productRepository.findByNaturalKey("shvl804", "LP", "UK"))
                .willReturn(Optional.of(productWithId(11L)));
        given(productRepository.findByNaturalKey("cl1355", "LP", "US"))
                .willReturn(Optional.of(productWithId(12L)));
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(false);

        // when
        seedLoader.run();

        // then
        then(priceHistoryRepository).should(times(2 * TRADES_PER_PRODUCT)).save(any());
    }

    @Test
    @DisplayName("컨디션 6등급에 등급별 6건씩 분산되고, 좋은 등급이 나쁜 등급보다 항상 비싸다")
    void run_컨디션_분산과_가격_질서() {
        // given
        givenAllTargetProductsExist();
        given(priceHistoryRepository.existsByAuctionId(anyLong())).willReturn(false);

        // when
        seedLoader.run();

        // then — 상품 하나(10L)만 떼어 확인
        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        then(priceHistoryRepository).should(times(3 * TRADES_PER_PRODUCT)).save(captor.capture());
        List<PriceHistory> ofProduct = captor.getAllValues().stream()
                .filter(trade -> trade.getProductId() == 10L)
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

    @Test
    @DisplayName("시세 시드가 찾는 대상 상품은 모두 상품 시드 원장에 있다")
    void targets_상품_원장에_존재() {
        // given — 두 시드는 (카탈로그번호+포맷+국가)로 연결된다. 원장에서 대상이 빠지면 시세가 조용히 비어 데모가 깨진다
        for (PriceHistorySeedLoader.SeedTarget target : PriceHistorySeedLoader.TARGETS) {
            // when
            boolean exists = ProductSeedData.PRODUCTS.stream()
                    .anyMatch(product -> Objects.equals(TextNormalizer.normalize(product.catalogNumber()),
                            TextNormalizer.normalize(target.catalogNumber()))
                            && product.format().equals(target.format())
                            && product.releaseCountry().equals(target.releaseCountry()));

            // then
            assertThat(exists)
                    .as("상품 원장에 없음: %s — 시세 시드가 이 상품을 못 찾아 건너뛴다", target.catalogNumber())
                    .isTrue();
        }
    }
}
