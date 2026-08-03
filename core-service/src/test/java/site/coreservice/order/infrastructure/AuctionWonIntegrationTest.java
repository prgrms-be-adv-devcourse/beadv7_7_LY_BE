package site.coreservice.order.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import site.common.event.EventPublisher;
import site.common.event.contract.AuctionWonEvent;
import site.coreservice.order.application.port.ProductInfo;
import site.coreservice.order.application.port.ProductPort;
import site.coreservice.order.domain.OrderRepository;

/**
 * Kafka 배선(리스너 등록·역직렬화·컨슈머 처리)을 실 브로커+DB로 검증한다. 단위 테스트로는 잡히지 않는 영역. 실행 전제: docker/local MySQL +
 * Kafka 기동, application-local.yml에 ddl-auto: update(로컬 전용, 커밋 금지).
 * <p>
 * 발행은 Kafka로 나가고 컨슈머 스레드가 비동기로 처리하므로 발행 직후 즉시 단언할 수 없다 — Awaitility로 "처리가 끝났다고 볼 수 있는 신호"를 기다린 뒤
 * 단언한다.
 * <p>
 * {@code auction.won}은 {@code order-service} 그룹 하나만 구독 중이라(order.completed처럼 여러 바운디드 컨텍스트가 팬아웃으로 받는
 * 구조가 아님) 다른 컨텍스트 리스너를 목으로 끊어둘 필요가 없다.
 * <p>
 * 상품 조회는 목으로 대신한다 — 여기서 보려는 건 리스너·서비스·DB 사이의 배선이지 상품 조회 응답 해석이 아니다(그건 ProductHttpClientTest가 따로
 * 덮는다).
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("local")
class AuctionWonIntegrationTest {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final List<Long> TEST_AUCTION_IDS = List.of(701L, 702L, 704L);

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @MockitoBean
    private ProductPort productPort;

    @MockitoSpyBean
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        orderJpaRepository.findAll().stream()
            .filter(order -> TEST_AUCTION_IDS.contains(order.getAuctionId()))
            .forEach(orderJpaRepository::delete);
    }

    private AuctionWonEvent auctionWon(Long auctionId) {
        return AuctionWonEvent.builder()
            .auctionId(auctionId)
            .productId(1L)
            .winnerId(2L)
            .sellerId(3L)
            .itemCondition("MINT")
            .firstImageUrl("https://example.com/1.png")
            .winningPrice(BigDecimal.valueOf(15_000))
            .build();
    }

    @Test
    @DisplayName("발행하면 컨슈머가 비동기로 받아 주문을 생성한다")
    void 발행_후_주문_생성됨() {
        // given
        given(productPort.getProductInfo(1L))
            .willReturn(new ProductInfo("Abbey Road", "비틀즈", 1969, "ORIGINAL"));

        // when
        eventPublisher.publish(auctionWon(701L));

        // then
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
            assertThat(orderJpaRepository.existsByAuctionId(701L)).isTrue());
    }

    @Test
    @DisplayName("같은 이벤트를 두 번 발행해도 주문은 한 개만 남는다")
    void 중복_발행_주문_한개() {
        // given
        given(productPort.getProductInfo(1L))
            .willReturn(new ProductInfo("Abbey Road", "비틀즈", 1969, "ORIGINAL"));

        // when
        eventPublisher.publish(auctionWon(702L));
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
            assertThat(orderJpaRepository.existsByAuctionId(702L)).isTrue());
        eventPublisher.publish(auctionWon(702L));

        // then — auction_id 유니크 제약이 있어 "있음" = "정확히 한 개". 두 번째 발행이 예외 없이
        // 건너뛰었는지가 핵심이라, 두 번째 메시지도 처리됐다는 신호(조기 리턴 체크 2회 호출)까지 기다린다.
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
            verify(orderRepository, times(2)).existsByAuctionId(702L));
        assertThat(orderJpaRepository.existsByAuctionId(702L)).isTrue();
    }

    @Test
    @DisplayName("동시 경합 경로 — 사전 조회를 속여 유니크 제약에 부딪혀도 오류 없이 건너뛴다")
    void 동시_경합_경로_제약_위반_후_건너뜀() {
        // given: 주문을 먼저 만들어 두고(처리 완료까지 대기), 사전 조회만 "없음"으로 속여
        // 두 번째 저장 시도가 진짜 제약 위반에 부딪히게 한다
        given(productPort.getProductInfo(1L))
            .willReturn(new ProductInfo("Abbey Road", "비틀즈", 1969, "ORIGINAL"));
        eventPublisher.publish(auctionWon(704L));
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
            assertThat(orderJpaRepository.existsByAuctionId(704L)).isTrue());
        willReturn(false).willCallRealMethod().given(orderRepository).existsByAuctionId(anyLong());

        // when: 리스너 방벽 덕에 예외가 새지 않고, 서비스는 DataIntegrityViolationException을 잡아
        // 로그만 남기고 정상 종료해야 한다
        eventPublisher.publish(auctionWon(704L));

        // then: 두 번째 메시지도 처리됐다는 신호를 기다린 뒤, 주문은 여전히 하나뿐인지 확인
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
            verify(orderRepository, times(2)).existsByAuctionId(704L));
        assertThat(orderJpaRepository.existsByAuctionId(704L)).isTrue();
    }
}
