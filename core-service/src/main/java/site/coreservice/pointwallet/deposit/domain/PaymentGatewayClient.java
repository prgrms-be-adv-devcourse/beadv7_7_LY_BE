package site.coreservice.pointwallet.deposit.domain;
import site.coreservice.pointwallet.shared.Money;

public interface PaymentGatewayClient {
    PgApproveResult approve(String providerTxId, String orderId, Money amount);
    PgCancelResult cancel(String providerTxId, String cancelReason, Money cancelAmount);
}