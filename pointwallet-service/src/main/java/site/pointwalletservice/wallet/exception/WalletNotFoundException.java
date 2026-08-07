package site.pointwalletservice.wallet.exception;

import site.common.exception.BusinessException;

/** 존재하지 않는 유저의 지갑을 조회했을 때. 호출한 컨텍스트(Deposit, Hold 등)가 자기 ErrorCode로 번역해서 다시 던진다. */
public class WalletNotFoundException extends BusinessException {
    public WalletNotFoundException() {
        super(WalletErrorCode.WALLET_NOT_FOUND);
    }
}