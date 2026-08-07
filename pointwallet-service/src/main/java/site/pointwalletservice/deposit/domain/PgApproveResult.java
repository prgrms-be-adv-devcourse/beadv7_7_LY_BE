package site.pointwalletservice.deposit.domain;
import site.pointwalletservice.shared.Money;

public record PgApproveResult(String providerTxId, String orderId, Money approvedAmount) {}