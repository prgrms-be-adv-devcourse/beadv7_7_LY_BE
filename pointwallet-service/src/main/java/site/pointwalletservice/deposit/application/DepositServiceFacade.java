package site.pointwalletservice.deposit.application;

import java.lang.reflect.UndeclaredThrowableException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryException;
import org.springframework.stereotype.Service;
import site.pointwalletservice.shared.Money;

/**
 * HoldServiceFacade/WithdrawServiceFacade와 동일한 패턴 — 자세한 이유는 그쪽 클래스 주석 참고.
 * RetryingDepositService(@Retryable)를 호출하고, 재시도가 다 소진돼서 던져지는
 * UndeclaredThrowableException(RetryException을 감싼 것)을 풀어 원래 도메인 예외
 * (DepositLockContentionException 등)로 다시 던진다.
 * <p>
 * @Primary는 여기 있다 — RetryingDepositService는 재시도 로직만 담당하는 내부 구현 세부사항이고,
 * 실제로 주입받아 쓰이는 건 이 파사드다.
 */
@Primary
@Service
@RequiredArgsConstructor
public class DepositServiceFacade implements DepositService {

    private final RetryingDepositService retryingDepositService;

    @Override
    public DepositRequestResult requestDeposit(Long userId, Money amount) {
        return retryingDepositService.requestDeposit(userId, amount);
    }

    @Override
    public void confirmDeposit(String providerTxId, String orderId, Money amount) {
        try {
            retryingDepositService.confirmDeposit(providerTxId, orderId, amount);
        } catch (UndeclaredThrowableException e) {
            throw unwrap(e);
        }
    }

    @Override
    public void cancelDeposit(Long depositId, String reason) {
        retryingDepositService.cancelDeposit(depositId, reason);
    }

    private RuntimeException unwrap(UndeclaredThrowableException e) {
        Throwable undeclared = e.getUndeclaredThrowable();
        if (undeclared instanceof RetryException retryException
                && retryException.getCause() instanceof RuntimeException cause) {
            return cause;
        }
        return e;
    }
}