package site.pointwalletservice.withdraw.application;
import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;
import site.pointwalletservice.withdraw.exception.WithdrawLockContentionException;

/**
 * requestWithdraw()에서 사용자 본인 지갑 락 경합(WithdrawLockContentionException)이 나면,
 * requestWithdraw() 호출 자체를 처음부터 다시 시도한다. RetryingHoldService와 동일한 이유로
 * 재시도를 WithdrawApplicationService 내부가 아니라 이 바깥 레이어에서 한다.
 * jitter=25는 RetryingHoldService 벤치마크로 검증한 값을 그대로 가져왔다.
 */
@Service
@RequiredArgsConstructor
public class RetryingWithdrawService implements WithdrawService {

    private final WithdrawApplicationService withdrawApplicationService;

    @Override
    @Retryable(
            includes = WithdrawLockContentionException.class,
            maxRetries = 5,
            delay = 50,
            jitter = 25,
            multiplier = 2,
            maxDelay = 800
    )
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount) {
        return withdrawApplicationService.requestWithdraw(userId, amount);
    }

    @Override
    public WithdrawStatusResult getStatus(Long withdrawRequestId) {
        return withdrawApplicationService.getStatus(withdrawRequestId);
    }
}