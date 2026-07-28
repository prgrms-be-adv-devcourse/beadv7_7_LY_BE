package site.coreservice.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import site.common.event.EventPublisher;
import site.coreservice.global.event.OrderCompletedEvent;
import site.coreservice.product.domain.AuctionSnapshotPort;
import site.coreservice.product.domain.ClosedAuction;
import site.coreservice.product.domain.MediaCondition;
import site.coreservice.product.domain.PriceHistoryRepository;

/**
 * 스프링 배선(리스너 등록·REQUIRES_NEW·커밋 후 발화)을 실 DB로 검증한다. 단위 테스트로는 잡히지 않는 영역.
 * 실행 전제: docker/local MySQL 기동 + application-local.yml에 ddl-auto: update (로컬 전용, 커밋 금지).
 * 수신 리스너는 진짜 커밋에만 반응하므로 테스트 트랜잭션 자동 롤백은 쓸 수 없다 — 수동 정리한다.
 * <p>
 * 경매 조회는 목으로 대신한다. 여기서 보려는 건 리스너·서비스·DB 사이의 배선이고, 응답 해석은
 * AuctionSnapshotHttpClientTest가 따로 덮는다. 목이 대체하는 건 조회 창구 빈이라 HTTP 요청이 나가지
 * 않는다(RestClient 빈 자체는 컨텍스트에 그대로 뜬다). 덕분에 실행에 경매 데이터가 필요 없다.
 * <p>
 * 가짜 발행 플래그는 강제로 끈다 — local yml에 켜 둔 상태로 테스트를 돌리면 컨텍스트 기동 때
 * 다른 경로가 먼저 적재해 행 수 단언이 어긋난다.
 */
@Tag("integration")
@SpringBootTest(properties = "product.fake-trade.enabled=false")
@ActiveProfiles("local")
class PriceHistoryIntegrationTest {

    private static final Long PRODUCT_ID = 1L;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private PriceHistoryJpaRepository priceHistoryJpaRepository;

    @MockitoBean
    private AuctionSnapshotPort auctionSnapshotPort;

    @MockitoSpyBean
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        given(auctionSnapshotPort.findClosedAuction(anyLong()))
                .willAnswer(invocation -> Optional.of(closedAuction(invocation.getArgument(0))));
    }

    @AfterEach
    void tearDown() {
        priceHistoryJpaRepository.deleteAll();
    }

    private ClosedAuction closedAuction(Long auctionId) {
        return new ClosedAuction(auctionId, PRODUCT_ID, MediaCondition.MINT, 15_000L, 3,
                LocalDateTime.of(2026, 7, 20, 20, 31), "ENDED_WON");
    }

    private OrderCompletedEvent orderCompleted(Long auctionId) {
        return new OrderCompletedEvent(1L, auctionId, 2L, 3L, BigDecimal.valueOf(15_000), LocalDateTime.now());
    }

    private void publishInTransaction(OrderCompletedEvent event, boolean rollback) {
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
        OrderCompletedEvent event = orderCompleted(501L);

        // when
        publishInTransaction(event, false);

        // then
        assertThat(priceHistoryJpaRepository.findByAuctionId(501L)).isPresent();
    }

    @Test
    @DisplayName("같은 이벤트를 두 번 발행해도 행은 한 개만 남는다")
    void 중복_발행_행_한개() {
        // given
        OrderCompletedEvent event = orderCompleted(502L);

        // when
        publishInTransaction(event, false);
        publishInTransaction(orderCompleted(502L), false);

        // then
        assertThat(priceHistoryJpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("발행 후 롤백되면 시세 행이 생기지 않는다")
    void 롤백_시_행_없음() {
        // given-when
        publishInTransaction(orderCompleted(503L), true);

        // then
        assertThat(priceHistoryJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("경매를 찾을 수 없는 이벤트는 발행자를 깨뜨리지 않고 행도 남기지 않는다")
    void 경매없음_발행자_보호() {
        // given
        given(auctionSnapshotPort.findClosedAuction(90_001L)).willReturn(Optional.empty());

        // when: 예외가 새면 이 호출 자체가 던진다
        publishInTransaction(orderCompleted(90_001L), false);

        // then
        assertThat(priceHistoryJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("마감되지 않은 경매의 이벤트도 발행자를 깨뜨리지 않고 행을 남기지 않는다")
    void 미마감_발행자_보호() {
        // given
        given(auctionSnapshotPort.findClosedAuction(90_501L)).willReturn(Optional.of(
                new ClosedAuction(90_501L, PRODUCT_ID, MediaCondition.MINT, 15_000L, 3,
                        LocalDateTime.of(2026, 7, 20, 20, 31), "RUNNING")));

        // when
        publishInTransaction(orderCompleted(90_501L), false);

        // then
        assertThat(priceHistoryJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("동시 경합 경로 — 사전 조회를 속여 유니크 제약에 부딪혀도 오류 없이 건너뛴다")
    void 경합_경로_제약_위반_후_건너뜀() {
        // given: 행을 먼저 만들어 두고, 사전 조회만 "없음"으로 속여 저장 시도가 진짜 제약 위반에 부딪히게 한다
        publishInTransaction(orderCompleted(504L), false);
        willReturn(Optional.empty()).willCallRealMethod().given(priceHistoryRepository).findByAuctionId(anyLong());

        // when: 리스너 방벽 덕에 예외가 새지 않고, 서비스는 재확인 후 정상 종료해야 한다
        publishInTransaction(orderCompleted(504L), false);

        // then: 행은 여전히 한 개
        assertThat(priceHistoryJpaRepository.count()).isEqualTo(1);
    }
}
