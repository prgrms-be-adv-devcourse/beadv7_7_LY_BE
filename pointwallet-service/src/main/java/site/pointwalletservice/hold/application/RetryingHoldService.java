package site.pointwalletservice.hold.application;
import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import site.pointwalletservice.hold.exception.HoldLockContentionException;
import site.pointwalletservice.shared.Money;

/**
 * hold()에서 지갑 락 경합(HoldLockContentionException)이 나면, hold() 호출 자체를 처음부터 다시
 * 시도한다. Hold 행(auction_id) 락 경합(HoldRowLockContentionException)은 대상이 아니다 - 그건
 * auction-service가 즉시 알아야 하는 신호라 여기서 흡수하지 않는다(HoldRowLockContentionException
 * 클래스 주석 참고).
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
 * org.springframework.core.retry.RetryException으로 감싸는데, 그 언래핑은 이 클래스가 아니라
 * HoldServiceFacade가 한다 - AOP 프록시 특성상 이 예외는 hold() 메서드 몸체 실행 도중이 아니라
 * "이 메서드를 호출한 쪽"에서 던져지므로, 같은 클래스 안에서는(자기 자신을 감싼 프록시 예외를
 * 자기가 볼 수 없어서) 잡을 수가 없다.
 * <p>
 * release()/consume()은 재시도하지 않는다 - Kafka 리스너가 이미 at-least-once 재전송으로
 * 재시도를 보장하고, 멱등하게 처리되므로 여기서 추가로 감쌀 필요가 없다(HoldApplicationService 참고).
 * <p>
 * @Primary는 HoldServiceFacade 쪽에 있다 - 이 클래스는 재시도 로직만 담당하는 내부 구현 세부사항이고,
 * 실제로 주입받아 쓰이는 건 그 파사드다.
 */
@Service
@RequiredArgsConstructor
public class RetryingHoldService implements HoldService {

    private final HoldApplicationService holdApplicationService;

    @Override
    @Retryable(
            includes = HoldLockContentionException.class,
            maxRetries = 5,
            delay = 50,
            jitter = 25,
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