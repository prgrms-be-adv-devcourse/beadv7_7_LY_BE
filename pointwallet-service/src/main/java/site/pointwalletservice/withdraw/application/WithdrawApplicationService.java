package site.pointwalletservice.withdraw.application;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.outbox.application.OutboxEventStore;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletLockFailedException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;
import site.pointwalletservice.withdraw.application.port.BankAccount;
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
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount) {
        // 1) 계좌 유효성 확인 — 트랜잭션 밖. 저장은 안 함(그때그때 조회만)
        BankAccount bankAccount = memberBankAccountPort.getBankAccount(userId)
                .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.BANK_ACCOUNT_NOT_FOUND));

        // 2) 수수료 계산 (2%, 내림) - PLATFORM_USER_ID 본인은 이 플로우 대상이 아니므로 예외 처리 불필요
        Money feeAmount = amount.multiply(WITHDRAW_FEE_RATE);
        Money netAmount = amount.subtract(feeAmount);

        // 3) 사용자 지갑 차감 + DB 반영 + 수수료 이벤트 Outbox 저장 — 전부 하나의 짧은 트랜잭션.
        // bankAccount는 이번 스텝에서 검증 용도로만 사용.
        Withdraw withdraw = transactionTemplate.execute(status -> {
            WalletBalanceResult userResult;
            try {
                userResult = walletService.deduct(userId, amount);
            } catch (WalletNotFoundException e) {
                throw new WithdrawException(WithdrawErrorCode.WALLET_NOT_FOUND);
            } catch (InsufficientBalanceException e) {
                throw new WithdrawException(WithdrawErrorCode.INSUFFICIENT_BALANCE);
            } catch (WalletLockFailedException e) {
                throw new WithdrawLockContentionException();
            }

            Withdraw w = withdrawRepository.save(Withdraw.request(userId, amount, feeAmount, netAmount));
            pointTransactionService.record(
                    userResult.walletId(), PointTransactionType.WITHDRAW, amount, userResult.balanceAfter(), w.getId()
            );

            outboxEventStore.store(
                    WithdrawFeeEarnedEvent.TOPIC,
                    w.getId().toString(),
                    new WithdrawFeeEarnedEvent(w.getId(), feeAmount.getValue())
            );

            // 실제 뱅킹 연동 없이 즉시 성공 처리 (시뮬레이션)
            w.complete();
            return w;
        });

        return WithdrawRequestResult.from(withdraw);
    }

    @Override
    public WithdrawStatusResult getStatus(Long withdrawRequestId) {
        Withdraw withdraw = withdrawRepository.findById(withdrawRequestId)
                .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.WITHDRAW_NOT_FOUND));
        return WithdrawStatusResult.from(withdraw);
    }
}