package site.coreservice.pointwallet.deposit.domain;
import site.coreservice.pointwallet.shared.Money;

public record TossCancelResult(String paymentKey, String transactionKey, Money canceledAmount) {}