package site.coreservice.pointwallet.withdraw.exception;
import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class WithdrawException extends BusinessException {
    public WithdrawException(ErrorCode errorCode) {
        super(errorCode);
    }
}