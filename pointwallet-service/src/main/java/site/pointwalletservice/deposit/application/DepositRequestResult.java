package site.pointwalletservice.deposit.application;
import site.pointwalletservice.shared.Money;

public record DepositRequestResult(String orderId, Money amount) {}