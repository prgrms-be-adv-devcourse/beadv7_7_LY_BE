package site.coreservice.pointwallet.deposit.domain;

import site.coreservice.pointwallet.shared.Money;

public interface TossPaymentsClient {
    TossConfirmResult confirmPayment(String paymentKey, String orderId, Money amount);

    TossCancelResult cancelPayment(String paymentKey, String cancelReason, Money cancelAmount);
}