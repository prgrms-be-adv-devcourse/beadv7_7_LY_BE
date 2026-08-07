package site.pointwalletservice.deposit.domain;
import site.pointwalletservice.shared.Money;

public record PgCancelResult(String providerTxId, String providerCancelTxId, Money canceledAmount) {}