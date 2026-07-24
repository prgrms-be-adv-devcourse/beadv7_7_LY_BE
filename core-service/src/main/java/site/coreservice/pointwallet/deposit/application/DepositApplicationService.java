package site.coreservice.pointwallet.deposit.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.pointwallet.deposit.domain.Deposit;
import site.coreservice.pointwallet.deposit.exception.DepositErrorCode;
import site.coreservice.pointwallet.deposit.exception.DepositException;
import site.coreservice.pointwallet.deposit.domain.DepositRepository;
import site.coreservice.pointwallet.deposit.domain.TossConfirmResult;
import site.coreservice.pointwallet.deposit.domain.TossPaymentsClient;
import site.coreservice.pointwallet.ledger.application.PointTransactionService;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.application.WalletBalanceResult;
import site.coreservice.pointwallet.wallet.application.WalletService;
import site.coreservice.pointwallet.wallet.domain.InsufficientBalanceException;
import site.coreservice.pointwallet.wallet.exception.WalletNotFoundException;

@Service
@RequiredArgsConstructor
public class DepositApplicationService implements DepositService {

    private final DepositRepository depositRepository;
    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;
    private final TossPaymentsClient tossPaymentsClient;

    @Override
    @Transactional
    public DepositRequestResult requestDeposit(Long userId, Money amount) {
        String orderId = generateOrderId();
        Deposit deposit = Deposit.request(userId, orderId, amount);
        depositRepository.save(deposit);
        return new DepositRequestResult(orderId, amount);
    }

    @Override
    @Transactional
    public void confirmDeposit(String paymentKey, String orderId, Money callbackAmount) {
        Deposit deposit = depositRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DepositException(DepositErrorCode.DEPOSIT_NOT_FOUND));

        if (!deposit.getRequestedAmount().equals(callbackAmount)) {
            deposit.fail();
            throw new DepositException(DepositErrorCode.AMOUNT_MISMATCH);
        }

        TossConfirmResult result = tossPaymentsClient.confirmPayment(paymentKey, orderId, callbackAmount);
        deposit.confirm(result.paymentKey(), result.orderId(), result.approvedAmount());

        creditWallet(deposit.getUserId(), result.approvedAmount(), deposit.getId());
    }

    /** 충전 확정된 금액을 지갑 잔액에 반영하고, 그 사실을 원장에 기록한다. */
    private void creditWallet(Long userId, Money amount, Long depositId) {
        WalletBalanceResult result = walletService.charge(userId, amount);

        pointTransactionService.record(
                result.walletId(), PointTransactionType.DEPOSIT, amount, result.balanceAfter(), depositId
        );
    }

    @Override
    @Transactional
    public void cancelDeposit(Long depositId, String reason) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new DepositException(DepositErrorCode.DEPOSIT_NOT_FOUND));

        // 취소 가능 여부(DONE 상태인지)는 Deposit 스스로 검증하고, 아니면 자기 컨텍스트의 예외를 직접 던진다.
        deposit.cancel(reason);

        // 지갑 조회·차감·잔액부족 검증은 전부 WalletService(→Wallet) 책임. 여기서는 그 결과로 온 예외를
        // Deposit 컨텍스트의 비즈니스 예외로 번역만 한다 - Hold 쪽과 동일한 패턴.
        WalletBalanceResult result;
        try {
            result = walletService.deduct(deposit.getUserId(), deposit.getRequestedAmount());
        } catch (WalletNotFoundException e) {
            throw new DepositException(DepositErrorCode.WALLET_NOT_FOUND);
        } catch (InsufficientBalanceException e) {
            throw new DepositException(DepositErrorCode.CANCEL_INSUFFICIENT_BALANCE);
        }

        // 내부 검증(상태·잔액)을 모두 통과한 뒤에야 되돌릴 수 없는 외부 호출을 한다.
        tossPaymentsClient.cancelPayment(deposit.getPaymentKey(), reason, deposit.getRequestedAmount());

        pointTransactionService.record(
                result.walletId(), PointTransactionType.DEPOSIT_CANCEL, deposit.getRequestedAmount(),
                result.balanceAfter(), deposit.getId()
        );
    }

    private String generateOrderId() {
        return "DEPOSIT-" + UUID.randomUUID();
    }
}