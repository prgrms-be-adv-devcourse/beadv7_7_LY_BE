package site.pointwalletservice.withdraw.application;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.outbox.application.OutboxEventStore;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.shared.PlatformAccount;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletLockFailedException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;
import site.pointwalletservice.withdraw.application.port.MemberBankAccountPort;
import site.pointwalletservice.withdraw.domain.Withdraw;
import site.pointwalletservice.withdraw.domain.WithdrawRepository;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;
import site.pointwalletservice.withdraw.exception.WithdrawLockContentionException;

/**
 * requestWithdraw()는 @Transactional을 메서드에 걸지 않는다 - 1) 계좌 조회(외부 HTTP 호출)를
 * 트랜잭션 밖에서 먼저 끝내고, 2) 사용자 지갑 차감+DB 반영+Outbox 저장만 TransactionTemplate으로
 * 짧은 트랜잭션에 담는다.
 * <p>
 * 멱등키 처리는 두 겹으로 방어한다:
 * ① 사전 조회(findExisting) - Facade가 재시도 루프 진입 전에 호출, 정상 시나리오(중복 클릭/
 *    네트워크 재시도)는 대부분 여기서 걸러진다.
 * ② DB 유니크 제약(user_id, idempotency_key) - 사전 조회 시점과 실제 저장 시점 사이의 레이스는
 *    막지 못하므로, executeDeductionAndOutbox()에서 DataIntegrityViolationException을 잡아
 *    (커밋 실패로 지갑 차감을 포함한 트랜잭션 전체가 이미 자동 롤백된 뒤) 기존 결과로 수렴시킨다.
 *    Deposit 확정 로직(confirmDeposit)에서 이미 검증된 패턴 그대로 재사용 - catch를
 *    transactionTemplate.execute() 호출 "바깥"에 둔다. unique 위반은 보통 커밋 시점 flush에서
 *    터지므로, 람다 안에서 잡으려 하면 이미 늦다.
 * <p>
 * 조회는 반드시 userId를 함께 대조한다 - idempotencyKey 단독 조회는 다른 사용자의 키 문자열을
 * 그대로 보내는 요청이 그 사람의 인출 결과를 그대로 돌려받는 경로(BOLA)를 만든다.
 */
@Service
@RequiredArgsConstructor
public class WithdrawApplicationService implements WithdrawService {

    private static final BigDecimal WITHDRAW_FEE_RATE = BigDecimal.valueOf(0.02);

    private final WithdrawRepository withdrawRepository;
    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;
    private final MemberBankAccountPort memberBankAccountPort;
    private final TransactionTemplate transactionTemplate;
    private final OutboxEventStore outboxEventStore;

    @Override
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount, String idempotencyKey) {
        // TODO 계좌 검증 롤백 및 계좌 사용 로직 추가 #351
        //validateBankAccount(userId);
        Withdraw withdraw = executeDeductionAndOutbox(userId, amount, idempotencyKey);
        return WithdrawRequestResult.from(withdraw);
    }

    /**
     * (userId, idempotencyKey) 조합으로 이미 처리된 요청이 있는지 조회한다. userId를 반드시
     * 함께 대조해야 한다 - 그렇지 않으면 남의 키를 보낸 요청이 남의 인출 결과를 그대로 받아간다.
     */
    public Optional<Withdraw> findExisting(Long userId, String idempotencyKey) {
        return withdrawRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    /**
     * 계좌 유효성 확인 — 트랜잭션 밖, 재시도 대상 밖. 저장은 안 함(그때그때 조회만).
     */
    public void validateBankAccount(Long userId) {
        memberBankAccountPort.getBankAccount(userId)
                .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.BANK_ACCOUNT_NOT_FOUND));
    }

    /**
     * 사용자 지갑 차감 + DB 반영 + 수수료 이벤트 Outbox 저장 — 전부 하나의 짧은 트랜잭션.
     * WithdrawLockContentionException이 나면 RetryingWithdrawService가 이 메서드만 다시 부른다.
     * 유니크 제약 위반(DataIntegrityViolationException)은 락 경합과 다른 문제라 재시도하지 않고,
     * 트랜잭션 커밋 실패(자동 롤백 포함) 이후 곧바로 기존 결과 조회로 수렴시킨다.
     */
    public Withdraw executeDeductionAndOutbox(Long userId, Money amount, String idempotencyKey) {
        Money feeAmount = amount.multiply(WITHDRAW_FEE_RATE);
        Money netAmount = amount.subtract(feeAmount);

        try {
            return transactionTemplate.execute(status ->
                    deductAndSave(userId, idempotencyKey, amount, feeAmount, netAmount));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 (userId, idempotencyKey)로 들어온 다른 요청이 레이스로 먼저 커밋을
            // 마침 - 이 트랜잭션(지갑 차감 포함)은 커밋 실패로 이미 자동 롤백된 상태.
            return withdrawRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.WITHDRAW_NOT_FOUND));
        }
    }

    private Withdraw deductAndSave(Long userId, String idempotencyKey, Money amount, Money feeAmount, Money netAmount) {
        WalletBalanceResult userResult = deductOrThrowLockContention(userId, amount);
        Withdraw w = withdrawRepository.save(Withdraw.request(userId, idempotencyKey, amount, feeAmount, netAmount));
        recordLedgerAndOutbox(w, userResult, amount, feeAmount);
        return w;
    }

    private WalletBalanceResult deductOrThrowLockContention(Long userId, Money amount) {
        try {
            return walletService.deduct(userId, amount);
        } catch (WalletNotFoundException e) {
            throw new WithdrawException(WithdrawErrorCode.WALLET_NOT_FOUND);
        } catch (InsufficientBalanceException e) {
            throw new WithdrawException(WithdrawErrorCode.INSUFFICIENT_BALANCE);
        } catch (WalletLockFailedException e) {
            throw new WithdrawLockContentionException();
        }
    }

    private void recordLedgerAndOutbox(Withdraw w, WalletBalanceResult userResult, Money amount, Money feeAmount) {
        pointTransactionService.record(
                userResult.walletId(), PointTransactionType.WITHDRAW, amount, userResult.balanceAfter(), w.getId()
        );

        outboxEventStore.store(
                WithdrawFeeEarnedEvent.TOPIC,
                PlatformAccount.PLATFORM_USER_ID.toString(),   // ← 항상 같은 키
                new WithdrawFeeEarnedEvent(w.getId(), feeAmount.getValue())
        );

        // 실제 뱅킹 연동 없이 즉시 성공 처리 (시뮬레이션)
        w.complete();
    }

    @Override
    public WithdrawStatusResult getStatus(Long withdrawRequestId) {
        Withdraw withdraw = withdrawRepository.findById(withdrawRequestId)
                .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.WITHDRAW_NOT_FOUND));
        return WithdrawStatusResult.from(withdraw);
    }
}