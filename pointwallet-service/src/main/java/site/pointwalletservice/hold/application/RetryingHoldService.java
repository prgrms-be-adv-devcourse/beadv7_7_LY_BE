package site.pointwalletservice.hold.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import site.pointwalletservice.hold.exception.HoldLockContentionException;
import site.pointwalletservice.shared.Money;

/**
 * hold()에서 Hold 행 락/지갑 락이 경합(NOWAIT 즉시실패)으로 막히면, hold() 호출 자체를 처음부터
 * 다시 시도한다.
 * <p>
 * 재시도를 HoldApplicationService.hold() 안(예: catch 블록에서 Thread.sleep 후 재조회)이 아니라
 * 이 바깥 레이어에서 하는 이유 - 안에서 하면 이미 열려있는 트랜잭션(그리고 그 트랜잭션이 쥔 DB
 * 커넥션)을 붙잡은 채로 대기(backoff)하게 된다. 여기서는 매 시도가 hold()를 처음부터 다시
 * 호출하는 것이라, 실패한 시도의 트랜잭션은 이미 롤백되어 커넥션이 반납된 상태로 backoff에 들어간다.
 * MySQL을 여러 서비스가 하나의 인스턴스로 같이 쓰고 있어서, 이 차이가 다른 서비스의 커넥션 여유에도
 * 영향을 준다.
 * <p>
 * @Retryable은 스프링 부트 4 / 프레임워크 7의 core 내장 재시도 기능이다(org.springframework.resilience) -
 * 구 spring-retry 라이브러리(org.springframework.retry)는 스프링 부트 4부터 BOM 버전 관리 대상에서
 * 빠지고 core 기능으로 흡수됐다. 재시도가 다 소진되면 원래 예외를 그대로 던지지 않고
 * org.springframework.core.retry.RetryException으로 감싸서 던지므로,
 * site.common.exception.GlobalExceptionHandler에서 그 예외를 풀어서(getCause) 원래
 * BusinessException으로 처리하도록 같이 손봐뒀다 - 안 그러면 HERR-3005 대신 GERR-0001로 뭉개진다.
 * <p>
 * release()/consume()은 재시도하지 않는다 - Kafka 리스너가 이미 at-least-once 재전송으로
 * 재시도를 보장하고, 멱등하게 처리되므로 여기서 추가로 감쌀 필요가 없다(HoldApplicationService 참고).
 * <p>
 * @Primary로 등록해서 HoldController 등 HoldService를 주입받는 곳은 코드 변경 없이 이 데코레이터를
 * 먼저 타게 한다. 실제 로직은 HoldApplicationService에 그대로 있다.
 */
@Primary
@Service
@RequiredArgsConstructor
public class RetryingHoldService implements HoldService {

    private final HoldApplicationService holdApplicationService;

    @Override
    @Retryable(
            includes = HoldLockContentionException.class,
            maxRetries = 5,
            delay = 50,
            multiplier = 2,
            maxDelay = 800
    )
    public HoldResult hold(Long auctionId, Long userId, Money amount) {
        return holdApplicationService.hold(auctionId, userId, amount);
    }

    @Override
    public void release(Long auctionId) {
        holdApplicationService.release(auctionId);
    }

    @Override
    public void consume(Long auctionId) {
        holdApplicationService.consume(auctionId);
    }
}