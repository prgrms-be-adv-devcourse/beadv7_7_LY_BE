package site.coreservice.pointwallet.deposit.application;

import site.coreservice.pointwallet.shared.Money;

public interface DepositService {

    DepositRequestResult requestDeposit(Long userId, Money amount);

    void confirmDeposit(String paymentKey, String orderId, Money amount);
}