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
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.domain.Wallet;
import site.coreservice.pointwallet.wallet.domain.WalletRepository;

@Service
@RequiredArgsConstructor
public class DepositApplicationService implements DepositService {

    private final DepositRepository depositRepository;
    private final WalletRepository walletRepository;
    private final PointTransactionRepository pointTransactionRepository;
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

    /** 충전 확정된 금액을 지갑 잔액에 반영하고, 그 사실을 원장에 append한다. */
    private void creditWallet(Long userId, Money amount, Long depositId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.open(userId)));

        wallet.charge(amount);
        walletRepository.save(wallet);

        PointTransaction transaction = PointTransaction.record(
                wallet.getId(), PointTransactionType.DEPOSIT, amount, wallet.getBalance(), depositId
        );
        pointTransactionRepository.save(transaction);
    }

    private String generateOrderId() {
        return "DEPOSIT-" + UUID.randomUUID();
    }
}