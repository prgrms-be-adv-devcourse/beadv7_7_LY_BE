package site.coreservice.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import site.common.event.EventPublisher;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.PriceHistoryRepository;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.ProductRepository;
import site.coreservice.product.domain.TradeConfirmedEvent;

/**
 * 스프링 배선(리스너 등록·REQUIRES_NEW·커밋 후 발화)을 실 DB로 검증한다. 단위 테스트로는 잡히지 않는 영역.
 * 실행 전제: docker/local MySQL 기동 + application-local.yml에 ddl-auto: update (로컬 전용, 커밋 금지).
 * 수신 리스너는 진짜 커밋에만 반응하므로 테스트 트랜잭션 자동 롤백은 쓸 수 없다 — 수동 정리한다.
 * <p>
 * 가짜 발행 플래그는 강제로 끈다 — local yml에 켜 둔 상태로 테스트를 돌리면 컨텍스트 기동 때
 * 시드 러너가 수백 건을 먼저 적재해 행 수 단언이 전부 어긋난다.
 */
@SpringBootTest(properties = "product.fake-trade.enabled=false")
@ActiveProfiles("local")
class TradeConfirmedIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceHistoryJpaRepository priceHistoryJpaRepository;

    @MockitoSpyBean
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        priceHistoryJpaRepository.deleteAll();
    }

    private Long anyProductId() {
        return productRepository.findAllActiveIds().stream().findFirst()
                .orElseGet(() -> productRepository.save(Product.of("IT 0001", 1L, "통합테스트반", "KR", 2026,
                        PressType.ORIGINAL, "LP", null, null, null, null)).getId());
    }

    private void publishInTransaction(TradeConfirmedEvent event, boolean rollback) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            eventPublisher.publish(event);
            if (rollback) {
                status.setRollbackOnly();
            }
        });
    }

    @Test
    @DisplayName("트랜잭션 안에서 발행하고 커밋하면 시세 행이 실제로 저장된다")
    void 커밋_후_행_저장() {
        // given
        anyProductId();
        TradeConfirmedEvent event = new TradeConfirmedEvent(501L, LocalDateTime.now());

        // when
        publishInTransaction(event, false);

        // then
        assertThat(priceHistoryJpaRepository.findByAuctionId(501L)).isPresent();
    }

    @Test
    @DisplayName("같은 이벤트를 두 번 발행해도 행은 한 개만 남는다")
    void 중복_발행_행_한개() {
        // given
        anyProductId();
        TradeConfirmedEvent event = new TradeConfirmedEvent(502L, LocalDateTime.now());

        // when
        publishInTransaction(event, false);
        publishInTransaction(new TradeConfirmedEvent(502L, LocalDateTime.now()), false);

        // then
        assertThat(priceHistoryJpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("발행 후 롤백되면 시세 행이 생기지 않는다")
    void 롤백_시_행_없음() {
        // given
        anyProductId();

        // when
        publishInTransaction(new TradeConfirmedEvent(503L, LocalDateTime.now()), true);

        // then
        assertThat(priceHistoryJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("경매를 찾을 수 없는 이벤트는 발행자를 깨뜨리지 않고 행도 남기지 않는다")
    void 실패_대역_발행자_보호() {
        // given: 90000번대는 스텁이 "경매 없음"을 돌려주는 예약 대역
        anyProductId();

        // when: 예외가 새면 이 호출 자체가 던진다
        publishInTransaction(new TradeConfirmedEvent(90_001L, LocalDateTime.now()), false);

        // then
        assertThat(priceHistoryJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("동시 경합 경로 — 사전 조회를 속여 유니크 제약에 부딪혀도 오류 없이 건너뛴다")
    void 경합_경로_제약_위반_후_건너뜀() {
        // given: 행을 먼저 만들어 두고, 사전 조회만 "없음"으로 속여 저장 시도가 진짜 제약 위반에 부딪히게 한다
        anyProductId();
        publishInTransaction(new TradeConfirmedEvent(504L, LocalDateTime.now()), false);
        willReturn(Optional.empty()).willCallRealMethod().given(priceHistoryRepository).findByAuctionId(anyLong());

        // when: 리스너 방벽 덕에 예외가 새지 않고, 서비스는 재확인 후 정상 종료해야 한다
        publishInTransaction(new TradeConfirmedEvent(504L, LocalDateTime.now()), false);

        // then: 행은 여전히 한 개
        assertThat(priceHistoryJpaRepository.count()).isEqualTo(1);
    }
}
