package site.pointwalletservice.deposit.domain;
import site.pointwalletservice.shared.Money;

public record PgInquiryResult(String providerTxId, String orderId, Money totalAmount, Money balanceAmount, String status) {}