package site.pointwalletservice.wallet.exception;

import site.common.exception.BusinessException;

/** 존재하지 않는 id로 인출 수수료 DLT 기록을 조회/재처리하려 했을 때. */
public class WithdrawFeeDeadLetterNotFoundException extends BusinessException {
    public WithdrawFeeDeadLetterNotFoundException() {
        super(WalletErrorCode.WITHDRAW_FEE_DEAD_LETTER_NOT_FOUND);
    }
}