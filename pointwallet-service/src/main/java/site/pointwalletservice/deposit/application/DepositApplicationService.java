package site.pointwalletservice.deposit.application;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.deposit.domain.Deposit;
import site.pointwalletservice.deposit.domain.DepositRepository;
import site.pointwalletservice.deposit.domain.DepositStatus;
import site.pointwalletservice.deposit.domain.PaymentGatewayClient;
import site.pointwalletservice.deposit.domain.PgApproveResult;
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

        if (!deposit.getRequestedAmount().equals(callbackAmount)) {
            transactionTemplate.executeWithoutResult(status -> {
                deposit.fail();
                depositRepository.save(deposit);
            });
            throw new DepositException(DepositErrorCode.AMOUNT_MISMATCH);
        }

        if (deposit.getStatus() != DepositStatus.REQUESTED) {
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
                log.error("보정 취소마저 실패 - 수동 확인 필요. orderId={}, providerTxId={}",
                        orderId, result.providerTxId(), cancelFailure);
            }
            throw e;
        }
    }

    @Override
    public void cancelDeposit(Long depositId, String reason) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DepositErrorCode.DEPOSIT_NOT_FOUND));

        if (deposit.getStatus() != DepositStatus.DONE) {
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
                log.error("차감 보정마저 실패 - 수동 확인 필요. depositId={}", depositId, compensateFailure);
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
            // PG 취소는 이미 성공(실제 환불됨) — 되돌릴 게 없으니 로그만 남기고 수동/배치 확인
            log.error("PG 취소 성공 후 DB 반영 실패 - 이미 환불된 건, 수동 확인 필요. depositId={}", depositId, e);
            throw e;
        }
    }

    private String generateOrderId() {
        return "DEPOSIT-" + UUID.randomUUID();
    }
}