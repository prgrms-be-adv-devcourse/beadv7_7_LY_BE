package site.coreservice.product.infrastructure.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.product.domain.ClosedAuction;
import site.coreservice.product.domain.MediaCondition;
import site.coreservice.product.domain.PriceHistory;
import site.coreservice.product.domain.PriceHistoryRepository;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.ProductRepository;
import site.coreservice.product.domain.TextNormalizer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 시세 화면 데모용 거래 기록 시드 로더. local 프로파일 + {@code product.price-seed.enabled=true}일 때만
 * 동작한다(기본 OFF). 상품 시드(@Order(1))가 만든 실재 상품을 카탈로그번호로 찾아 그 상품 id로 기록을 쌓는다
 * — 경매 시드를 거치지 않는 이유는 경매 시드의 상품 id(1001~)가 실재 상품과 겹치지 않아, 그 경로로 쌓으면
 * 기록은 있는데 시세 조회가 상품을 못 찾아 404가 나기 때문이다.
 * <p>
 * 일부러 대상 3개 상품에만 넣는다 — 나머지 상품은 "거래 기록 없음 = 200 + 빈 배열" 응답도 화면에서
 * 보여줘야 하므로 비워둔다. 상품당 건수(36)는 시세 조회 상한(100건)보다 작게 유지한다.
 * <p>
 * 여러 번 실행해도 안전하다. 시드 거래의 경매 id는 미리 정해둔 번호대(910000~)에서 계산으로 정해지므로,
 * 이미 같은 경매 id의 기록이 있으면 그 건은 건너뛴다.
 */
@Slf4j
@Order(3)
@Profile("local")
@ConditionalOnProperty(prefix = "product.price-seed", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class PriceHistorySeedLoader implements CommandLineRunner {

    /**
     * 시드 거래가 쓰는 경매 id 시작 번호. price_history의 경매 id는 두 번 쌓이는 것을 유니크 제약으로 막고
     * 있어서, 진짜 경매가 이 번호까지 자라면 그 거래의 기록이 거부된다 — 로컬에서 도달할 일 없는 크기로 둔다.
     */
    static final long SEED_AUCTION_ID_BASE = 910_000L;

    /** 등급별 거래 수. 6등급 × 6건 = 상품당 36건 — 등급별 평균·최저·최고가가 의미 있게 나오는 크기. */
    static final int TRADES_PER_CONDITION = 6;

    static final List<SeedTarget> TARGETS = List.of(
            new SeedTarget("PCS 7088", "LP", "UK", 350_000L),  // Abbey Road 1969 오리지널
            new SeedTarget("SHVL 804", "LP", "UK", 280_000L),  // The Dark Side of the Moon 1973 오리지널
            new SeedTarget("CL 1355", "LP", "US", 800_000L));  // Kind of Blue 1959 오리지널

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        int saved = 0;
        for (int targetIndex = 0; targetIndex < TARGETS.size(); targetIndex++) {
            SeedTarget target = TARGETS.get(targetIndex);
            Optional<Product> product = productRepository.findByNaturalKey(
                    TextNormalizer.normalize(target.catalogNumber()), target.format(), target.releaseCountry());
            if (product.isEmpty()) {
                log.warn("[PriceHistorySeedLoader] 상품이 없어 건너뜀: {} — 상품 시드(product.seed.enabled)를 먼저 켰는지 확인",
                        target.catalogNumber());
                continue;
            }
            saved += seedTrades(targetIndex, target, product.get().getId(), now);
        }
        log.info("[PriceHistorySeedLoader] 시세 시드 적재 완료 — 신규 {}건 (여러 번 실행해도 안전)", saved);
    }

    private int seedTrades(int targetIndex, SeedTarget target, Long productId, LocalDateTime now) {
        int saved = 0;
        MediaCondition[] conditions = MediaCondition.values();
        for (int conditionIndex = 0; conditionIndex < conditions.length; conditionIndex++) {
            for (int sequence = 0; sequence < TRADES_PER_CONDITION; sequence++) {
                long auctionId = SEED_AUCTION_ID_BASE + targetIndex * 1_000L + conditionIndex * 100L + sequence;
                if (priceHistoryRepository.existsByAuctionId(auctionId)) {
                    continue;
                }
                MediaCondition condition = conditions[conditionIndex];
                LocalDateTime tradedAt = tradedAt(now, targetIndex, conditionIndex, sequence);
                ClosedAuction auction = new ClosedAuction(auctionId, productId, condition,
                        finalPrice(target.basePrice(), condition, sequence), bidCount(conditionIndex, sequence),
                        tradedAt, "ENDED_WON");
                priceHistoryRepository.save(PriceHistory.of(auction, tradedAt.plusMinutes(30)));
                saved++;
            }
        }
        return saved;
    }

    /**
     * 등급이 좋을수록 비싸고(MINT 125% ~ POOR 30%), 같은 등급 안에서도 회차마다 -6%~+9%로 오르내려
     * 평균·최저·최고가가 서로 다른 값이 나오게 한다. 값은 계산으로만 정해지므로 실행할 때마다 같다.
     */
    private Long finalPrice(long basePrice, MediaCondition condition, int sequence) {
        int variancePercent = (sequence - 2) * 3;
        long price = basePrice * conditionPercent(condition) * (100 + variancePercent) / 10_000;
        return price / 1_000 * 1_000;
    }

    private int conditionPercent(MediaCondition condition) {
        return switch (condition) {
            case MINT -> 125;
            case NEAR_MINT -> 100;
            case VERY_GOOD_PLUS -> 80;
            case VERY_GOOD -> 62;
            case GOOD -> 45;
            case POOR -> 30;
        };
    }

    /** 최근 91일 안에 거래가 고르게 흩어지도록 회차·등급·상품마다 다른 날짜를 만든다. 미래 시각은 나오지 않는다. */
    private LocalDateTime tradedAt(LocalDateTime now, int targetIndex, int conditionIndex, int sequence) {
        int daysAgo = 1 + sequence * 15 + conditionIndex * 2 + targetIndex;
        return now.minusDays(daysAgo).minusHours(conditionIndex * 3L + sequence);
    }

    private Integer bidCount(int conditionIndex, int sequence) {
        return 3 + (conditionIndex + sequence) % 7;
    }

    /** 시세를 넣을 대상 상품을 찾는 열쇠(카탈로그번호+포맷+발매국가)와 그 상품의 기준가. */
    record SeedTarget(String catalogNumber, String format, String releaseCountry, long basePrice) {
    }
}
