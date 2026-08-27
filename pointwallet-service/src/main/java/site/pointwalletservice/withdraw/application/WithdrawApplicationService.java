package site.pointwalletservice.withdraw.application;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
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
import site.pointwalletservice.withdraw.domain.WithdrawStatus;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;
import site.pointwalletservice.withdraw.exception.WithdrawLockContentionException;

/**
 * requestWithdraw()는 @Transactional을 메서드에 걸지 않는다 - 1) 계좌 조회(외부 HTTP 호출)를
 * 트랜잭션 밖에서 먼저 끝내고, 2) 사용자 지갑 차감+DB 반영+Outbox 저장만 TransactionTemplate으로
 * 짧은 트랜잭션에 담는다.
 * <p>
 * executeDeductionAndOutbox()는 DepositApplicationService의 4단계 보상 흐름과 같은 원칙으로,
 * "지갑 차감 → 멱등키 저장(또는 기존 결과 반환) → 원장 기록+Outbox 저장"이라는 순서를 이름 있는
 * private 메서드 호출로만 보여준다. 각 단계의 구체적인 예외 번역·롤백 방법은 해당 메서드 안에 감춘다.
 * <p>
 * 수수료 적립은 이 트랜잭션 안에서 직접 charge()를 부르지 않고, WithdrawFeeEarnedEvent를
 * OutboxEventStore로 같은 트랜잭션에 저장해둔다 - 지갑 차감이 커밋되는데 이벤트 저장은 실패하는
 * (혹은 그 반대) 상황이 트랜잭션 원자성으로 원천 차단된다. 실제 Kafka 발행은 OutboxRelay가
 * 별도로 폴링하며 처리하고, 플랫폼 계정 반영은 WithdrawFeeEarnedEventHandler(wallet 쪽 컨슈머)가
 * 순차적으로 한다.
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
        validateBankAccount(userId);
        Withdraw withdraw = executeDeductionAndOutbox(userId, amount, idempotencyKey);
        return WithdrawRequestResult.from(withdraw);
    }

    /**
     * 계좌 유효성 확인 — 트랜잭션 밖, 재시도 대상 밖. 저장은 안 함(그때그때 조회만).
     * 이 검증은 락 경합과 무관하므로, 호출부(WithdrawServiceFacade)에서 재시도 루프 진입 전에
     * 딱 한 번만 호출한다.
     */
    public void validateBankAccount(Long userId) {
        memberBankAccountPort.getBankAccount(userId)
                .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.BANK_ACCOUNT_NOT_FOUND));
    }

    /**
     * 멱등키로 이미 처리된 요청인지 확인한다 — 저장소 접근은 애플리케이션 계층의 책임이므로,
     * 파사드가 WithdrawRepository를 직접 참조하지 않고 이 메서드를 통해서만 조회하게 한다.
     */
    public Optional<WithdrawRequestResult> findByIdempotencyKey(String idempotencyKey) {
        return withdrawRepository.findByIdempotencyKey(idempotencyKey).map(WithdrawRequestResult::from);
    }

    /**
     * 사용자 지갑 차감 + DB 반영 + 수수료 이벤트 Outbox 저장 — 전부 하나의 짧은 트랜잭션.
     * WithdrawLockContentionException이 나면 RetryingWithdrawService가 이 메서드만 다시 부른다 -
     * 계좌 조회 같은 외부 호출이 섞여있지 않아, 재시도가 반복돼도 부가 비용 없이 트랜잭션만 다시 탄다.
     */
    public Withdraw executeDeductionAndOutbox(Long userId, Money amount, String idempotencyKey) {
        Money feeAmount = amount.multiply(WITHDRAW_FEE_RATE);
        Money netAmount = amount.subtract(feeAmount);

        return transactionTemplate.execute(status -> {
            WalletBalanceResult userResult = deductOrThrowLockContention(userId, amount);
            Withdraw withdraw = saveOrReturnExisting(status, userId, amount, feeAmount, netAmount, idempotencyKey);

            if (withdraw.getStatus() == WithdrawStatus.SUCCESS) {
                // saveOrReturnExisting()이 유니크 제약 충돌로 기존 건을 돌려준 경우 - 이미 완결된
                // 건이라 원장 기록/Outbox 저장/complete()를 다시 실행하면 안 된다.
                return withdraw;
            }

            recordLedgerAndOutbox(withdraw, feeAmount, userResult);
            withdraw.complete(); // 실제 뱅킹 연동 없이 즉시 성공 처리 (시뮬레이션)
            return withdraw;
        });
    }

    // ===== executeDeductionAndOutbox 단계들 =====

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

    /**
     * 지갑 차감 결과를 Withdraw 행으로 저장한다. 같은 idempotencyKey로 동시에 들어온 다른 요청이
     * 이미 저장을 마쳤다면(레이스) 유니크 제약 위반이 나는데, 이 경우 방금 위에서 실행한 지갑 차감을
     * 되돌려야 하므로 트랜잭션을 rollback-only로 표시하고, 이미 커밋된 쪽의 결과를 그대로 반환한다 -
     * 사용자 입장에서는 어느 요청이 이겼든 결과가 동일해야 하기 때문이다.
     */
    private Withdraw saveOrReturnExisting(TransactionStatus status, Long userId, Money amount,
                                          Money feeAmount, Money netAmount, String idempotencyKey) {
        try {
            return withdrawRepository.save(Withdraw.request(userId, amount, feeAmount, netAmount, idempotencyKey));
        } catch (DataIntegrityViolationException e) {
            status.setRollbackOnly();
            return withdrawRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
        }
    }

    private void recordLedgerAndOutbox(Withdraw withdraw, Money feeAmount, WalletBalanceResult userResult) {
        pointTransactionService.record(
                userResult.walletId(), PointTransactionType.WITHDRAW,
                withdraw.getAmount(), userResult.balanceAfter(), withdraw.getId()
        );

        outboxEventStore.store(
                WithdrawFeeEarnedEvent.TOPIC,
                PlatformAccount.PLATFORM_USER_ID.toString(),   // ← 항상 같은 키
                new WithdrawFeeEarnedEvent(withdraw.getId(), feeAmount.getValue())
        );
    }

    // ===== 공통 =====

    @Override
    public WithdrawStatusResult getStatus(Long withdrawRequestId) {
        Withdraw withdraw = withdrawRepository.findById(withdrawRequestId)
                .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.WITHDRAW_NOT_FOUND));
        return WithdrawStatusResult.from(withdraw);
    }
}