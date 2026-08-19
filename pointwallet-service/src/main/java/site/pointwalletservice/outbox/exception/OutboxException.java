// outbox/exception/OutboxException.java
package site.pointwalletservice.outbox.exception;
import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class OutboxException extends BusinessException {
    public OutboxException(ErrorCode errorCode) {
        super(errorCode);
    }
}