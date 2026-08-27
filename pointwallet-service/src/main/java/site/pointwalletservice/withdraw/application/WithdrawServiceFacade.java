// withdraw/application/WithdrawServiceFacade.java
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
import site.pointwalletservice.withdraw.domain.Withdraw;

/**
 * requestWithdraw() 진입점. 계좌 검증(validateBankAccount)보다 먼저 (userId, idempotencyKey)
 * 조합으로 기존 처리 건을 조회한다 - 이미 처리된 요청이면 계좌 조회/재시도 루프 어느 쪽도 타지
 * 않고 바로 그 결과를 반환한다(외부 API 재호출·재차감 없음).
 * <p>
 * 조회 조건에 userId가 반드시 포함돼야 한다 - idempotencyKey만으로 조회하면 다른 사용자의 키
 * 문자열을 그대로 보내는 요청이 그 사람의 인출 결과(금액 포함)를 그대로 돌려받는 경로가 생긴다.
 * userId는 @MemberId(X-Member-Id 헤더, 인증된 값)에서 오므로 요청자 본인 것만 조회된다.
 * <p>
 * 계좌 검증(validateBankAccount)을 재시도 루프 진입 전에 1회만 실행하고, 그 뒤에 락 경합 재시도
 * 대상인 RetryingWithdrawService.requestWithdraw()(=지갑 차감 트랜잭션만)를 호출한다. 이 분리
 * 덕분에 락 경합이 몇 번을 반복되든 계좌 조회 API는 딱 한 번만 호출된다.
 * <p>
 * validateBankAccount()가 예외를 던지면(계좌 없음) 그 자체가 재시도 대상이 아니므로 바로
 * 전파된다 - WithdrawLockContentionException이 아닌 다른 WithdrawException은 애초에 재시도할
 * 이유가 없다.
 */
@Primary
@Service
@RequiredArgsConstructor
public class WithdrawServiceFacade implements WithdrawService {

    private final RetryingWithdrawService retryingWithdrawService;
    private final WithdrawApplicationService withdrawApplicationService;

    @Override
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount, String idempotencyKey) {
        Optional<Withdraw> existing = withdrawApplicationService.findExisting(userId, idempotencyKey);
        if (existing.isPresent()) {
            return WithdrawRequestResult.from(existing.get());
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