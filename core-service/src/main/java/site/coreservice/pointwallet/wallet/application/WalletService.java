package site.coreservice.pointwallet.wallet.application;
import site.coreservice.pointwallet.shared.Money;

public interface WalletService {

    /** 지갑이 없으면 새로 개설한 뒤 충전한다. (예치금 충전 등 신규 유입 흐름) */
    WalletBalanceResult charge(Long userId, Money amount);

    /** 지갑에 적립한다. 지갑이 없으면 WalletNotFoundException을 던진다 - charge()와 달리 자동 개설하지 않는다.
     * (홀드 해제처럼, 이미 지갑이 있었어야 하는 내부 환입 흐름에 쓴다.) */
    WalletBalanceResult credit(Long userId, Money amount);

    /** 지갑에서 차감한다. 지갑이 없으면 WalletNotFoundException, 잔액이 부족하면 InsufficientBalanceException을 던진다. */
    WalletBalanceResult deduct(Long userId, Money amount);
}