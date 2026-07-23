package site.coreservice.pointwallet.deposit.application;

import site.coreservice.pointwallet.shared.Money;

public record DepositRequestResult(String orderId, Money amount) {}