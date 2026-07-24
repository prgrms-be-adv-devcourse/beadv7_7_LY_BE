package site.coreservice.pointwallet.wallet.application;
import site.coreservice.pointwallet.shared.Money;

public interface WalletService {

    /** 지갑이 없으면 새로 개설한 뒤 충전한다. */
    WalletBalanceResult charge(Long userId, Money amount);

    /** 지갑에서 차감한다. 지갑이 없으면 WalletNotFoundException, 잔액이 부족하면 InsufficientBalanceException을 던진다. */
    WalletBalanceResult deduct(Long userId, Money amount);
}