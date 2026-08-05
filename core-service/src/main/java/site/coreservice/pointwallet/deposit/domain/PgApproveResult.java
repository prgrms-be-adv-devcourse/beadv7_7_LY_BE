package site.coreservice.pointwallet.deposit.domain;
import site.coreservice.pointwallet.shared.Money;

public record PgApproveResult(String providerTxId, String orderId, Money approvedAmount) {}