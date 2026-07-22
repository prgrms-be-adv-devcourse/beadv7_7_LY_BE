package site.coreservice.pointwallet.deposit.domain;

import site.coreservice.pointwallet.shared.Money;

public record TossConfirmResult(String paymentKey, String orderId, Money approvedAmount) {}