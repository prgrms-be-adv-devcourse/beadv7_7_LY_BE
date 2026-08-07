package site.productservice.infrastructure.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
import site.common.event.EventPublisher;
import site.common.event.contract.OrderCompletedEvent;
import site.productservice.application.port.AuctionSnapshotPort;
import site.productservice.domain.price.ClosedAuction;
import site.productservice.domain.price.MediaCondition;
import site.productservice.domain.price.PriceHistoryRepository;

/**
 * Kafka 배선(리스너 등록·역직렬화·컨슈머 처리)을 실 브로커+DB로 검증한다. 단위 테스트로는 잡히지 않는 영역.
 * 실행 전제: docker/local MySQL + Kafka 기동, application-local.yml에 ddl-auto: update(로컬 전용, 커밋 금지).
 * <p>
 * 발행은 Kafka로 나가고 컨슈머 스레드가 비동기로 처리하므로 발행 직후 즉시 단언할 수 없다 — Awaitility로
 * "처리가 끝났다고 볼 수 있는 신호"(행 생성, 혹은 조회 목 호출 횟수)를 기다린 뒤 단언한다.
 * <p>
 * 발행(OrderEventPublisher → KafkaEventPublisher)이 outbox 패턴 없이 트랜잭션과 무관하게 즉시
 * Kafka로 나가기 때문에, "발행자 트랜잭션이 롤백되면 반영 안 됨"이라는 이전 시나리오는 더 이상
 * 성립하지 않는다(PriceHistoryOrderCompletedListener 클래스 주석 참고) — 그 테스트는 제거했다.
 * outbox 도입 시 이 보장을 다시 테스트해야 한다.
 * <p>
 * 확인과 정리는 전부 이 테스트 전용 경매 id 기준으로 한다. 같은 테이블에 다른 데이터(데모 시드 등)가
 * 있어도 테스트가 흔들리지 않고, 그 데이터를 지우지도 않기 위해서다 — 전체 개수 단언과 전체 삭제는
 * 다른 컨텍스트가 먼저 적재해 두면 바로 깨진다.
 * <p>
 * 경매 조회는 목으로 대신한다. 여기서 보려는 건 리스너·서비스·DB 사이의 배선이고, 응답 해석은
 * AuctionSnapshotHttpClientTest가 따로 덮는다. 목이 대체하는 건 조회 창구 빈이라 HTTP 요청이 나가지
 * 않는다(RestClient 빈 자체는 컨텍스트에 그대로 뜬다). 덕분에 실행에 경매 데이터가 필요 없다. 부가로,
 * 컨슈머 처리가 끝났다는 신호로도 이 목의 호출 여부/횟수를 활용한다.
 * <p>
 * 시드 플래그는 강제로 끈다 — local yml에 켜 둔 상태로 테스트를 돌리면 컨텍스트 기동 때
 * 시드 로더가 먼저 적재해 행 수 단언이 어긋난다.
 */
@Tag("integration")
@SpringBootTest(properties = {
        "product.seed.enabled=false",
        "product.price-seed.enabled=false"
})
@ActiveProfiles("local")
class PriceHistoryIntegrationTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final List<Long> TEST_AUCTION_IDS = List.of(501L, 502L, 504L, 90_001L, 90_501L);

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private PriceHistoryJpaRepository priceHistoryJpaRepository;

    @MockitoBean
    private AuctionSnapshotPort auctionSnapshotPort;

    @MockitoSpyBean
    private PriceHistoryRepository priceHistoryRepository;

    @BeforeEach
    void setUp() {
        given(auctionSnapshotPort.findClosedAuction(anyLong()))
                .willAnswer(invocation -> Optional.of(closedAuction(invocation.getArgument(0))));
    }

    @AfterEach
    void tearDown() {
        TEST_AUCTION_IDS.forEach(auctionId -> priceHistoryJpaRepository.findByAuctionId(auctionId)
                .ifPresent(priceHistoryJpaRepository::delete));
    }

    private ClosedAuction closedAuction(Long auctionId) {
        return new ClosedAuction(auctionId, PRODUCT_ID, MediaCondition.MINT, 15_000L, 3,
                LocalDateTime.of(2026, 7, 20, 20, 31), "ENDED_WON");
    }

    private OrderCompletedEvent orderCompleted(Long auctionId) {
        return OrderCompletedEvent.builder()
                .orderId(1L)
                .auctionId(auctionId)
                .buyerId(2L)
                .sellerId(3L)
                .finalBidPrice(BigDecimal.valueOf(15_000))
                .completedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("발행하면 컨슈머가 비동기로 받아 시세 행을 저장한다")
    void 발행_후_행_저장() {
        // given
        OrderCompletedEvent event = orderCompleted(501L);

        // when
        eventPublisher.publish(event);

        // then
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
                assertThat(priceHistoryJpaRepository.findByAuctionId(501L)).isPresent());
    }

    @Test
    @DisplayName("같은 이벤트를 두 번 발행해도 행은 한 개만 남는다")
    void 중복_발행_행_한개() {
        // given
        OrderCompletedEvent event = orderCompleted(502L);

        // when
        eventPublisher.publish(event);
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
                assertThat(priceHistoryJpaRepository.findByAuctionId(502L)).isPresent());
        eventPublisher.publish(orderCompleted(502L));

        // then — auction_id 유니크 제약이 있어 "있음" = "정확히 한 개". 두 번째 발행이 예외 없이
        // 건너뛰었는지가 핵심이다. recordConfirmedTrade는 이미 기록된 거래면 auctionSnapshotPort를
        // 부르기도 전에 조기 리턴하므로(정상 동작), "처리 완료" 신호는 findClosedAuction이 아니라
        // 매번 무조건 호출되는 priceHistoryRepository.findByAuctionId 횟수로 잡는다.
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
                verify(priceHistoryRepository, times(2)).findByAuctionId(502L));
        assertThat(priceHistoryJpaRepository.findByAuctionId(502L)).isPresent();
    }

    @Test
    @DisplayName("경매를 찾을 수 없는 이벤트는 발행자를 깨뜨리지 않고 행도 남기지 않는다")
    void 경매없음_발행자_보호() {
        // given
        given(auctionSnapshotPort.findClosedAuction(90_001L)).willReturn(Optional.empty());

        // when
        eventPublisher.publish(orderCompleted(90_001L));

        // then: 처리가 끝났다는 신호(조회 호출)를 기다린 뒤 행이 없는지 확인
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
                verify(auctionSnapshotPort).findClosedAuction(90_001L));
        assertThat(priceHistoryJpaRepository.findByAuctionId(90_001L)).isEmpty();
    }

    @Test
    @DisplayName("마감되지 않은 경매의 이벤트도 발행자를 깨뜨리지 않고 행을 남기지 않는다")
    void 미마감_발행자_보호() {
        // given
        given(auctionSnapshotPort.findClosedAuction(90_501L)).willReturn(Optional.of(
                new ClosedAuction(90_501L, PRODUCT_ID, MediaCondition.MINT, 15_000L, 3,
                        LocalDateTime.of(2026, 7, 20, 20, 31), "RUNNING")));

        // when
        eventPublisher.publish(orderCompleted(90_501L));

        // then
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
                verify(auctionSnapshotPort).findClosedAuction(90_501L));
        assertThat(priceHistoryJpaRepository.findByAuctionId(90_501L)).isEmpty();
    }

    @Test
    @DisplayName("동시 경합 경로 — 사전 조회를 속여 유니크 제약에 부딪혀도 오류 없이 건너뛴다")
    void 경합_경로_제약_위반_후_건너뜀() {
        // given: 행을 먼저 만들어 두고(처리 완료까지 대기), 사전 조회만 "없음"으로 속여
        // 두 번째 저장 시도가 진짜 제약 위반에 부딪히게 한다
        eventPublisher.publish(orderCompleted(504L));
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
                assertThat(priceHistoryJpaRepository.findByAuctionId(504L)).isPresent());
        willReturn(Optional.empty()).willCallRealMethod().given(priceHistoryRepository).findByAuctionId(anyLong());

        // when: 리스너 방벽 덕에 예외가 새지 않고, 서비스는 재확인 후 정상 종료해야 한다
        eventPublisher.publish(orderCompleted(504L));

        // then: 두 번째 메시지도 처리됐다는 신호를 기다린 뒤, 행은 여전히 하나뿐인지 확인
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
                verify(auctionSnapshotPort, times(2)).findClosedAuction(504L));
        assertThat(priceHistoryJpaRepository.findByAuctionId(504L)).isPresent();
    }
}
