package site.coreservice.pointwallet.deposit.application;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.pointwallet.deposit.domain.Deposit;
import site.coreservice.pointwallet.deposit.exception.DepositErrorCode;
import site.coreservice.pointwallet.deposit.exception.DepositException;
import site.coreservice.pointwallet.deposit.domain.DepositRepository;
import site.coreservice.pointwallet.deposit.domain.PgApproveResult;
import site.coreservice.pointwallet.deposit.domain.PaymentGatewayClient;
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
    private final PaymentGatewayClient paymentGatewayClient;

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
    public void confirmDeposit(String providerTxId, String orderId, Money callbackAmount) {
        Deposit deposit = depositRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DepositException(DepositErrorCode.DEPOSIT_NOT_FOUND));

        if (!deposit.getRequestedAmount().equals(callbackAmount)) {
            deposit.fail();
            throw new DepositException(DepositErrorCode.AMOUNT_MISMATCH);
        }

        PgApproveResult result = paymentGatewayClient.approve(providerTxId, orderId, callbackAmount);
        deposit.confirm(result.providerTxId(), result.orderId(), result.approvedAmount());

        creditWallet(deposit.getUserId(), result.approvedAmount(), deposit.getId());
    }

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

        deposit.cancel(reason);

        WalletBalanceResult result;
        try {
            result = walletService.deduct(deposit.getUserId(), deposit.getRequestedAmount());
        } catch (WalletNotFoundException e) {
            throw new DepositException(DepositErrorCode.WALLET_NOT_FOUND);
        } catch (InsufficientBalanceException e) {
            throw new DepositException(DepositErrorCode.CANCEL_INSUFFICIENT_BALANCE);
        }

        paymentGatewayClient.cancel(deposit.getProviderTransactionId(), reason, deposit.getRequestedAmount());

        pointTransactionService.record(
                result.walletId(), PointTransactionType.DEPOSIT_CANCEL, deposit.getRequestedAmount(),
                result.balanceAfter(), deposit.getId()
        );
    }

    private String generateOrderId() {
        return "DEPOSIT-" + UUID.randomUUID();
    }
}