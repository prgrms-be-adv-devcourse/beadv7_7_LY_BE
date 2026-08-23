package site.pointwalletservice.hold.application;
import java.lang.reflect.UndeclaredThrowableException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryException;
import org.springframework.stereotype.Service;
import site.pointwalletservice.shared.Money;

/**
 * HoldService의 실제 주입 지점. RetryingHoldService(@Retryable)를 호출하고, 재시도가 다 소진돼서
 * 던져지는 예외를 풀어 원래 도메인 예외(HoldLockContentionException 등)로 다시 던진다.
 * <p>
 * 이 언래핑을 프레젠테이션 계층(HoldController)이 아니라 여기(애플리케이션 계층)에 두는 이유 -
 * "재시도 프레임워크가 예외를 감싼다"는 건 순전히 기술적인 디테일이라, 그걸 컨트롤러가 알아야 하는 건
 * 계층 책임이 뒤바뀐 것이다. HoldApplicationService가 이미 Wallet 도메인 예외를 Hold 도메인 예외로
 * 번역하는 책임을 지고 있는 것과 같은 원칙이다.
 * <p>
 * 왜 RetryException이 아니라 UndeclaredThrowableException을 잡는지 - RetryException은 checked
 * exception(Exception 상속)인데, HoldService.hold()는 그걸 throws로 선언하지 않고 있고
 * RetryingHoldService.hold()도(오버라이드 규칙상) 선언할 수 없다. 인터페이스가 선언 안 한 checked
 * exception을 프록시가 던지려 하면 스프링/JDK는 UndeclaredThrowableException(unchecked)으로 감싸서
 * 던진다 - 그래서 여기서 잡아야 하는 건 RetryException이 아니라 이거고, 그 안에서 진짜 RetryException을
 * 꺼내야 한다.
 * <p>
 * RetryingHoldService 자신이 이 언래핑을 못 하는 이유는 그쪽 클래스의 주석 참고 - AOP 프록시라서
 * 자기 메서드 안에서 자기를 감싼 예외를 못 잡는다.
 * <p>
 * @Primary는 여기 있다 - RetryingHoldService는 재시도 로직만 담당하는 내부 구현 세부사항이고,
 * 실제로 주입받아 쓰이는 건 이 파사드다.
 */
@Primary
@Service
@RequiredArgsConstructor
public class HoldServiceFacade implements HoldService {

    private final RetryingHoldService retryingHoldService;

    @Override
    public HoldResult hold(Long auctionId, Long userId, Money amount) {
        try {
            return retryingHoldService.hold(auctionId, userId, amount);
        } catch (UndeclaredThrowableException e) {
            throw unwrap(e);
        }
    }

    @Override
    public void release(Long auctionId) {
        retryingHoldService.release(auctionId);
    }

    @Override
    public void consume(Long auctionId) {
        retryingHoldService.consume(auctionId);
    }

    @Override
    public void rollback(Long holdId, Long auctionId, Long userId, Money amount) {
        try {
            retryingHoldService.rollback(holdId, auctionId, userId, amount);
        } catch (UndeclaredThrowableException e) {
            throw unwrap(e);
        }
    }

    private RuntimeException unwrap(UndeclaredThrowableException e) {
        Throwable undeclared = e.getUndeclaredThrowable();
        if (undeclared instanceof RetryException retryException
                && retryException.getCause() instanceof RuntimeException cause) {
            return cause;
        }
        // 예상 밖의 형태면(원인이 RuntimeException이 아니거나 등) 원인 추적이 가능하게 그대로 던진다.
        return e;
    }
}