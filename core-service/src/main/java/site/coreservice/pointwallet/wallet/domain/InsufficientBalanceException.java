package site.coreservice.pointwallet.wallet.domain;

/** Wallet 자신의 불변식(잔액은 음수가 될 수 없음) 위반. 호출한 컨텍스트가 자기 ErrorCode로 번역해서 다시 던진다. */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("잔액이 부족합니다.");
    }
}