package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import site.coreservice.product.application.FakeTradePublishService;

/**
 * 시세 화면 데모용으로 거래 기록을 대량으로 만드는 local 전용 러너. 상품(@Order(1))·경매(@Order(2)) 시드 다음에 돈다.
 * DB에 직접 넣지 않고 이벤트 발행 경로를 그대로 태운다 — 시드를 넣는 행위 자체가 수신·중복 방어
 * 로직의 반복 검증이 되고, 여러 번 실행해도 이미 기록된 경매는 건너뛰어 안전하다.
 * 경매 id는 1부터 순회 — 90000번대(실패 시연 예약 대역)와 겹치지 않아야 한다.
 */
@Slf4j
@Order(3)
@Profile("local")
@ConditionalOnProperty(name = "product.fake-trade.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class FakeTradeSeedRunner implements CommandLineRunner {

    private final FakeTradePublishService fakeTradePublishService;

    @Value("${product.fake-trade.seed-count:0}")
    private int seedCount;

    @Override
    public void run(String... args) {
        if (seedCount <= 0) {
            return;
        }
        for (long auctionId = 1; auctionId <= seedCount; auctionId++) {
            fakeTradePublishService.publishFakeTradeConfirmed(auctionId, null);
        }
        log.info("가짜 거래확정 {}건 발행 완료 (id 1~{})", seedCount, seedCount);
    }
}
