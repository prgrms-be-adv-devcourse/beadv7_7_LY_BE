package site.pointwalletservice.deposit.application;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import site.pointwalletservice.deposit.domain.Deposit;
import site.pointwalletservice.deposit.domain.DepositRepository;
import site.pointwalletservice.deposit.domain.PaymentGatewayClient;
import site.pointwalletservice.deposit.domain.PgApproveResult;
import site.pointwalletservice.deposit.domain.PgInquiryResult;
import site.pointwalletservice.deposit.exception.DepositErrorCode;
import site.pointwalletservice.deposit.exception.DepositException;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositApplicationService implements DepositService {

    private final DepositRepository depositRepository;
    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;
    private final PaymentGatewayClient paymentGatewayClient;
    private final TransactionTemplate transactionTemplate;

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
            transactionTemplate.executeWithoutResult(status -> {
                deposit.fail();
                depositRepository.save(deposit);
            });
            throw new DepositException(DepositErrorCode.AMOUNT_MISMATCH);
        }

        if (!deposit.isConfirmable()) {
            throw new DepositException(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }

        // PG 호출 — 트랜잭션 밖
        PgApproveResult result = paymentGatewayClient.approve(providerTxId, orderId, callbackAmount);

        try {
            // DB 반영 — PG 호출 없는 짧은 트랜잭션
            transactionTemplate.executeWithoutResult(status -> {
                deposit.confirm(result.providerTxId(), result.orderId(), result.approvedAmount());
                depositRepository.save(deposit);

                WalletBalanceResult walletResult = walletService.charge(deposit.getUserId(), result.approvedAmount());
                pointTransactionService.record(
                        walletResult.walletId(), PointTransactionType.DEPOSIT,
                        result.approvedAmount(), walletResult.balanceAfter(), deposit.getId()
                );
            });
        } catch (Exception e) {
            // PG는 이미 승인됨 — DB 반영 실패 → 즉시 보정 취소 시도
            log.error("PG 승인 성공 후 DB 반영 실패 - 보정 취소 시도. orderId={}, providerTxId={}",
                    orderId, result.providerTxId(), e);
            try {
                paymentGatewayClient.cancel(result.providerTxId(), "내부 저장 실패로 인한 자동 취소", result.approvedAmount());
                log.warn("보정 취소 성공. orderId={}", orderId);
            } catch (Exception cancelFailure) {
                logPgStateForManualCheck(orderId, result.providerTxId(), cancelFailure);
            }
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

        WalletBalanceResult deductResult;
        try {
            deductResult = transactionTemplate.execute(status ->
                    walletService.deduct(deposit.getUserId(), deposit.getRequestedAmount())
            );
        } catch (WalletNotFoundException e) {
            throw new DepositException(DepositErrorCode.WALLET_NOT_FOUND);
        } catch (InsufficientBalanceException e) {
            throw new DepositException(DepositErrorCode.CANCEL_INSUFFICIENT_BALANCE);
        }

        // PG 호출 — 트랜잭션 밖
        try {
            paymentGatewayClient.cancel(deposit.getProviderTransactionId(), reason, deposit.getRequestedAmount());
        } catch (Exception e) {
            // 지갑은 이미 차감됨 — PG 취소 실패 → 차감 보정(재충전) 시도
            log.error("지갑 차감 후 PG 취소 실패 - 차감 보정 시도. depositId={}", depositId, e);
            try {
                transactionTemplate.executeWithoutResult(status ->
                        walletService.charge(deposit.getUserId(), deposit.getRequestedAmount())
                );
                log.warn("차감 보정 성공. depositId={}", depositId);
            } catch (Exception compensateFailure) {
                logPgStateForManualCheck(String.valueOf(depositId), deposit.getProviderTransactionId(), compensateFailure);
            }
            throw e;
        }

        WalletBalanceResult finalWalletResult = deductResult;
        try {
            // PG 취소는 이미 성공 — DB 반영만 짧은 트랜잭션으로
            transactionTemplate.executeWithoutResult(status -> {
                deposit.cancel(reason);
                depositRepository.save(deposit);
                pointTransactionService.record(
                        finalWalletResult.walletId(), PointTransactionType.DEPOSIT_CANCEL,
                        deposit.getRequestedAmount(), finalWalletResult.balanceAfter(), deposit.getId()
                );
            });
        } catch (Exception e) {
            // PG 취소는 이미 성공(실제 환불됨) — 되돌릴 게 없으니 조회로 최신 상태 남기고 수동 확인
            log.error("PG 취소 성공 후 DB 반영 실패 - 이미 환불된 건, 수동 확인 필요. depositId={}", depositId, e);
            logPgStateForManualCheck(String.valueOf(depositId), deposit.getProviderTransactionId(), e);
            throw e;
        }
    }

    /**
     * 보정(재취소/재충전) 실패, 또는 PG 성공 후 DB 반영 실패 등 "사람이 직접 확인해야 하는" 상황에서
     * PG 조회 API로 실제 상태를 확인해 로그에 남긴다.
     * 실패 원인이 연결 자체 불가(ResourceAccessException)면 조회도 같은 이유로 실패할 가능성이 높으므로
     * 불필요한 재시도/대기 없이 생략하고, 원인이 구분되도록 로그를 남긴다.
     */
    private void logPgStateForManualCheck(String context, String providerTxId, Exception cause) {
        log.error("수동 확인 필요. context={}, providerTxId={}", context, providerTxId, cause);

        if (cause instanceof ResourceAccessException) {
            log.error("[수동확인용] Toss와 연결 자체가 불가능한 상태로 판단되어 조회를 생략함. context={}, providerTxId={}",
                    context, providerTxId);
            return;
        }

        try {
            PgInquiryResult inquiry = paymentGatewayClient.inquire(providerTxId);
            log.error("[수동확인용] PG 실제 상태 조회 결과 - context={}, providerTxId={}, orderId={}, status={}, totalAmount={}, balanceAmount={}",
                    context, providerTxId, inquiry.orderId(), inquiry.status(), inquiry.totalAmount(), inquiry.balanceAmount());
        } catch (Exception inquiryFailure) {
            log.error("[수동확인용] PG 상태 조회마저 실패 - providerTxId={}", providerTxId, inquiryFailure);
        }
    }

    private String generateOrderId() {
        return "DEPOSIT-" + UUID.randomUUID();
    }
}