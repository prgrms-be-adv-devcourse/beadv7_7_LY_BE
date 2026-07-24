package site.coreservice.pointwallet.wallet.application;
import site.coreservice.pointwallet.shared.Money;

public record WalletBalanceResult(Long walletId, Money balanceAfter) {}