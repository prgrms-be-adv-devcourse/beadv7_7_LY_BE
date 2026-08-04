package site.coreservice.pointwallet.deposit.domain;
import site.coreservice.pointwallet.shared.Money;

public record PgCancelResult(String providerTxId, String providerCancelTxId, Money canceledAmount) {}