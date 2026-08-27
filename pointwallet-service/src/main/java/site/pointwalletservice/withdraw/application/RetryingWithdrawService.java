package site.pointwalletservice.withdraw.application;
import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;
import site.pointwalletservice.withdraw.domain.Withdraw;
import site.pointwalletservice.withdraw.exception.WithdrawLockContentionException;

/**
 * 사용자 본인 지갑 락 경합(WithdrawLockContentionException)이 나면, executeDeductionAndOutbox()
 * (지갑 차감+DB 반영+Outbox 저장 트랜잭션)만 처음부터 다시 시도한다.
 * <p>
 * 계좌 조회(validateBankAccount)는 여기서 다루지 않는다 - 락 경합과 무관한 검증이라 호출부인
 * WithdrawServiceFacade가 재시도 루프 진입 전에 1회만 실행한다. 그래서 여기서 재시도되는 단위는
 * 순수하게 "지갑 차감 트랜잭션"뿐이고, 재시도 5번이 돌아도 외부 API를 다시 부르지 않는다.
 * <p>
 * idempotencyKey는 재시도되는 매 시도마다 그대로 전달된다 - 같은 요청의 재시도이므로 키가 바뀌면 안
 * 된다. executeDeductionAndOutbox() 내부의 유니크 제약 캐치 덕분에, 만약 재시도 도중 다른 스레드가
 * (드물게) 같은 키로 먼저 성공해도 안전하게 그 결과로 수렴한다.
 * <p>
 * jitter=25는 RetryingHoldService 벤치마크로 검증한 값을 그대로 가져왔다.
 */
@Service
@RequiredArgsConstructor
public class RetryingWithdrawService implements WithdrawService {

    private final WithdrawApplicationService withdrawApplicationService;

    @Retryable(
            includes = WithdrawLockContentionException.class,
            maxRetries = 5,
            delay = 50,
            jitter = 25,
            multiplier = 2,
            maxDelay = 800
    )
    @Override
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount, String idempotencyKey) {
        Withdraw withdraw = withdrawApplicationService.executeDeductionAndOutbox(userId, amount, idempotencyKey);
        return WithdrawRequestResult.from(withdraw);
    }

    @Override
    public WithdrawStatusResult getStatus(Long withdrawRequestId) {
        return withdrawApplicationService.getStatus(withdrawRequestId);
    }
}