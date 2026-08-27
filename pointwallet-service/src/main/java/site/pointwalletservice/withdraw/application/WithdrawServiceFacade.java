package site.pointwalletservice.withdraw.application;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryException;
import org.springframework.stereotype.Service;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;

/**
 * requestWithdraw() 진입점. 순수하게 "호출 순서 조정 + 재시도 예외 언래핑"만 담당한다 - 멱등키
 * 조회를 포함한 실제 판단 로직은 전부 WithdrawApplicationService(애플리케이션 계층)에 위임한다.
 * 파사드가 WithdrawRepository 같은 저장소를 직접 참조하면 안 되는 이유는 WithdrawFeeEarnedEventHandler
 * 등에서 이미 한 번 정리한 헥사고날 경계와 동일하다 - 저장소 접근은 항상 애플리케이션 서비스를 거친다.
 * <p>
 * 멱등키 확인을 계좌 검증보다도 앞에 두는 이유 - 이미 처리된 요청이면 외부 API(계좌 조회) 호출도
 * 불필요하다. 여기서 걸러지지 않는 동시 요청 레이스는 WithdrawApplicationService의 유니크 제약
 * 캐치가 최종 방어선이다.
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
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount, String idempotencyKey) {
        Optional<WithdrawRequestResult> existing = withdrawApplicationService.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        withdrawApplicationService.validateBankAccount(userId);
        try {
            return retryingWithdrawService.requestWithdraw(userId, amount, idempotencyKey);
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