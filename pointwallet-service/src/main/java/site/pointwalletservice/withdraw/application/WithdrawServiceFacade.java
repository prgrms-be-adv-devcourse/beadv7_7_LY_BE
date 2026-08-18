// withdraw/application/WithdrawServiceFacade.java
package site.pointwalletservice.withdraw.application;
import java.lang.reflect.UndeclaredThrowableException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryException;
import org.springframework.stereotype.Service;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;

/**
 * requestWithdraw() 진입점. 계좌 검증(validateBankAccount)을 재시도 루프 진입 전에 1회만 실행하고,
 * 그 뒤에 락 경합 재시도 대상인 RetryingWithdrawService.requestWithdraw()(=지갑 차감 트랜잭션만)를
 * 호출한다. 이 분리 덕분에 락 경합이 몇 번을 반복되든 계좌 조회 API는 딱 한 번만 호출된다.
 * <p>
 * validateBankAccount()가 예외를 던지면(계좌 없음) 그 자체가 재시도 대상이 아니므로 바로 전파된다 -
 * WithdrawLockContentionException이 아닌 다른 WithdrawException은 애초에 재시도할 이유가 없다.
 */
@Primary
@Service
@RequiredArgsConstructor
public class WithdrawServiceFacade implements WithdrawService {

    private final RetryingWithdrawService retryingWithdrawService;
    private final WithdrawApplicationService withdrawApplicationService;

    @Override
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount) {
        withdrawApplicationService.validateBankAccount(userId);
        try {
            return retryingWithdrawService.requestWithdraw(userId, amount);
        } catch (UndeclaredThrowableException e) {
            throw unwrap(e);
        }
    }

    @Override
    public WithdrawStatusResult getStatus(Long withdrawRequestId) {
        return retryingWithdrawService.getStatus(withdrawRequestId);
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