package site.pointwalletservice.withdraw.application;
import java.lang.reflect.UndeclaredThrowableException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryException;
import org.springframework.stereotype.Service;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;

@Primary
@Service
@RequiredArgsConstructor
public class WithdrawServiceFacade implements WithdrawService {

    private final RetryingWithdrawService retryingWithdrawService;

    @Override
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount) {
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