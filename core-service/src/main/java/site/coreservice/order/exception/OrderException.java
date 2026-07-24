package site.coreservice.order.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class OrderException extends BusinessException {

    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
