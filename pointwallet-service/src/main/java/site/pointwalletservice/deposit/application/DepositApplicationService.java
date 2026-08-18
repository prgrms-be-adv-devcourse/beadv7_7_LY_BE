package site.pointwalletservice.deposit.application;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.deposit.domain.Deposit;
import site.pointwalletservice.deposit.domain.DepositRepository;
import site.pointwalletservice.deposit.domain.PaymentGatewayClient;
import site.pointwalletservice.deposit.domain.PgApproveResult;
import site.pointwalletservice.deposit.exception.DepositErrorCode;
import site.pointwalletservice.deposit.exception.DepositException;
import site.pointwalletservice.deposit.reconciliation.application.DepositReconciliationLogRecorder;
import site.pointwalletservice.deposit.reconciliation.domain.ReconciliationFailureType;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;

/**
 * confirmDeposit()/cancelDeposit()은 각각 "PG 호출 → 짧은 DB 트랜잭션 반영 → 실패 시 보정 →
 * 보정마저 실패 시 정합성 로그"라는 동일한 모양의 4단계 보상 흐름(간이 saga)을 따른다. 최상위
 * 메서드는 이 네 단계를 이름 있는 private 메서드 호출로만 보여주고, 각 단계의 구체적인 방법
 * (트랜잭션 경계를 어디서 끊는지, PG를 어떻게 부르는지)은 해당 private 메서드 안에 감춘다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepositApplicationService implements DepositService {

    private final DepositRepository depositRepository;
    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;
    private final PaymentGatewayClient paymentGatewayClient;
    private final TransactionTemplate transactionTemplate;
    private final DepositReconciliationLogRecorder reconciliationLogRecorder;

    @Override
    @Transactional
    public DepositRequestResult requestDeposit(Long userId, Money amount) {
        String orderId = generateOrderId();
        Deposit deposit = Deposit.request(userId, orderId, amount);
        depositRepository.save(deposit);
        return new DepositRequestResult(orderId, amount);
    }

    @Override
    public void confirmDeposit(String providerTxId, String orderId, Money callbackAmount) {
        Deposit deposit = depositRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DepositException(DepositErrorCode.DEPOSIT_NOT_FOUND));

        if (!deposit.matchesAmount(callbackAmount)) {
            failDeposit(deposit);
            throw new DepositException(DepositErrorCode.AMOUNT_MISMATCH);
        }

        if (!deposit.isConfirmable()) {
            throw new DepositException(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }

        // PG 호출 — 트랜잭션 밖
        PgApproveResult result = paymentGatewayClient.approve(providerTxId, orderId, callbackAmount);

        try {
            applyConfirmedDeposit(deposit, result);
        } catch (Exception e) {
            log.error("PG 승인 성공 후 DB 반영 실패 - 보정 취소 시도. orderId={}, providerTxId={}",
                    orderId, result.providerTxId(), e);
            compensateConfirmFailure(deposit, result);
            throw e;
        }
    }

    @Override
    public void cancelDeposit(Long depositId, String reason) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DepositErrorCode.DEPOSIT_NOT_FOUND));

        if (!deposit.isCancelable()) {
            throw new DepositException(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }

        WalletBalanceResult deductResult = deductForCancel(deposit);

        // PG 호출 — 트랜잭션 밖
        try {
            paymentGatewayClient.cancel(deposit.getProviderTransactionId(), reason, deposit.getRequestedAmount());
        } catch (Exception e) {
            log.error("지갑 차감 후 PG 취소 실패 - 차감 보정 시도. depositId={}", depositId, e);
            compensateDeductionFailure(deposit);
            throw e;
        }

        try {
            applyCanceledDeposit(deposit, reason, deductResult);
        } catch (Exception e) {
            log.error("PG 취소 성공 후 DB 반영 실패 - 이미 환불된 건, 수동 확인 필요. depositId={}", depositId, e);
            recordReconciliationFailureSafely(depositId,
                    ReconciliationFailureType.CANCEL_DB_SAVE_FAILED, deposit.getProviderTransactionId(), e);
            throw e;
        }
    }

    // ===== confirmDeposit 단계들 =====

    /** DB 반영 — PG 호출 없는 짧은 트랜잭션. Deposit 확정 + 지갑 충전 + 원장 기록까지 한 번에. */
    private void applyConfirmedDeposit(Deposit deposit, PgApproveResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            deposit.confirm(result.providerTxId(), result.orderId(), result.approvedAmount());
            depositRepository.save(deposit);

            WalletBalanceResult walletResult = walletService.charge(deposit.getUserId(), result.approvedAmount());
            pointTransactionService.record(
                    walletResult.walletId(), PointTransactionType.DEPOSIT,
                    result.approvedAmount(), walletResult.balanceAfter(), deposit.getId()
            );
        });
    }

    /** PG는 이미 승인됨 — DB 반영 실패 → 즉시 보정 취소 시도. 보정마저 실패하면 정합성 로그로 넘긴다. */
    private void compensateConfirmFailure(Deposit deposit, PgApproveResult result) {
        try {
            paymentGatewayClient.cancel(result.providerTxId(), "내부 저장 실패로 인한 자동 취소", result.approvedAmount());
            log.warn("보정 취소 성공. orderId={}", deposit.getOrderId());
        } catch (Exception cancelFailure) {
            recordReconciliationFailureSafely(deposit.getId(),
                    ReconciliationFailureType.CONFIRM_COMPENSATION_FAILED, result.providerTxId(), cancelFailure);
        }
    }

    private void failDeposit(Deposit deposit) {
        transactionTemplate.executeWithoutResult(status -> {
            deposit.fail();
            depositRepository.save(deposit);
        });
    }

    // ===== cancelDeposit 단계들 =====

    /** 지갑 차감을 짧은 트랜잭션으로 먼저 반영하고, 도메인 예외를 DepositErrorCode로 번역한다. */
    private WalletBalanceResult deductForCancel(Deposit deposit) {
        try {
            return transactionTemplate.execute(status ->
                    walletService.deduct(deposit.getUserId(), deposit.getRequestedAmount())
            );
        } catch (WalletNotFoundException e) {
            throw new DepositException(DepositErrorCode.WALLET_NOT_FOUND);
        } catch (InsufficientBalanceException e) {
            throw new DepositException(DepositErrorCode.CANCEL_INSUFFICIENT_BALANCE);
        }
    }

    /** 지갑은 이미 차감됨 — PG 취소 실패 → 차감 보정(재충전) 시도. 보정마저 실패하면 정합성 로그로 넘긴다. */
    private void compensateDeductionFailure(Deposit deposit) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    walletService.charge(deposit.getUserId(), deposit.getRequestedAmount())
            );
            log.warn("차감 보정 성공. depositId={}", deposit.getId());
        } catch (Exception compensateFailure) {
            recordReconciliationFailureSafely(deposit.getId(),
                    ReconciliationFailureType.CANCEL_COMPENSATION_FAILED,
                    deposit.getProviderTransactionId(), compensateFailure);
        }
    }

    /** PG 취소는 이미 성공 — DB 반영(Deposit 취소 확정 + 원장 기록)만 짧은 트랜잭션으로. */
    private void applyCanceledDeposit(Deposit deposit, String reason, WalletBalanceResult finalWalletResult) {
        transactionTemplate.executeWithoutResult(status -> {
            deposit.cancel(reason);
            depositRepository.save(deposit);
            pointTransactionService.record(
                    finalWalletResult.walletId(), PointTransactionType.DEPOSIT_CANCEL,
                    deposit.getRequestedAmount(), finalWalletResult.balanceAfter(), deposit.getId()
            );
        });
    }

    // ===== 공통 =====

    /**
     * 안전망(정합성 로그) 기록 자체가 실패해도, 원래 터졌던 예외가 이 실패로 대체되어 호출부에
     * 전파되지 않는 일이 없도록 감싼다. 기록 실패는 로그로만 남기고 원래 흐름은 그대로 둔다.
     */
    private void recordReconciliationFailureSafely(Long depositId, ReconciliationFailureType failureType,
                                                   String providerTxId, Exception cause) {
        try {
            reconciliationLogRecorder.record(depositId, failureType, providerTxId, cause);
        } catch (Exception recordingFailure) {
            log.error("정합성 로그 기록 자체가 실패함 - depositId={}, failureType={}", depositId, failureType, recordingFailure);
        }
    }

    private String generateOrderId() {
        return "DEPOSIT-" + UUID.randomUUID();
    }
}