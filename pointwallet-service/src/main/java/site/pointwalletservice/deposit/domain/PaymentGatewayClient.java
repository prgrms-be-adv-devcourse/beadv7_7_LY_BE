package site.pointwalletservice.deposit.domain;
import site.pointwalletservice.shared.Money;

public interface PaymentGatewayClient {
    PgApproveResult approve(String providerTxId, String orderId, Money amount);
    PgCancelResult cancel(String providerTxId, String cancelReason, Money cancelAmount);
}