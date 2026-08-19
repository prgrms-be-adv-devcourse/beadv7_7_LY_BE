package site.pointwalletservice.deposit.application;

import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import site.pointwalletservice.deposit.exception.DepositLockContentionException;
import site.pointwalletservice.shared.Money;

/**
 * confirmDeposit()에서 지갑 락 경합(DepositLockContentionException)이 나면, confirmDeposit() 호출
 * 자체를 처음부터 다시 시도한다. PG 승인 호출까지 포함해서 재시도되지만, Toss 쪽이 Idempotency-Key
 * (DEPOSIT-CONFIRM-{orderId})로 같은 요청을 멱등하게 처리하므로 안전하다 — 재시도해도 실제 승인은
 * 한 번만 일어나고, 두 번째 시도는 이미 승인된 결과를 그대로 받는다.
 * <p>
 * requestDeposit()/cancelDeposit()은 이 경합 경로가 아니라 재시도 대상이 아니다.
 * <p>
 * Hold/Withdraw 도메인의 RetryingHoldService/RetryingWithdrawService와 동일한 패턴 — 재시도를
 * DepositApplicationService 안(트랜잭션을 쥔 채 대기)이 아니라 이 바깥 레이어에서 하는 이유도 동일:
 * 실패한 시도의 트랜잭션은 이미 롤백되어 커넥션이 반납된 상태로 backoff에 들어간다.
 */
@Service
@RequiredArgsConstructor
public class RetryingDepositService implements DepositService {

    private final DepositApplicationService depositApplicationService;

    @Override
    public DepositRequestResult requestDeposit(Long userId, Money amount) {
        return depositApplicationService.requestDeposit(userId, amount);
    }

    @Override
    @Retryable(
            includes = DepositLockContentionException.class,
            maxRetries = 5,
            delay = 50,
            jitter = 25,
            multiplier = 2,
            maxDelay = 800
    )
    public void confirmDeposit(String providerTxId, String orderId, Money amount) {
        depositApplicationService.confirmDeposit(providerTxId, orderId, amount);
    }

    @Override
    public void cancelDeposit(Long depositId, String reason) {
        depositApplicationService.cancelDeposit(depositId, reason);
    }
}