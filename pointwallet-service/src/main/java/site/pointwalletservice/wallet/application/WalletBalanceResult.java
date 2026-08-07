package site.pointwalletservice.wallet.application;
import site.pointwalletservice.shared.Money;

public record WalletBalanceResult(Long walletId, Money balanceAfter) {}