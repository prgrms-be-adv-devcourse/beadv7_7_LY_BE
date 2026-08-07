package site.pointwalletservice.deposit.presentation.dto;
import site.pointwalletservice.deposit.exception.DepositErrorCode;
import site.pointwalletservice.deposit.exception.DepositException;

public record DepositCancelRequest(String reason) {

    public DepositCancelRequest {
        if (reason == null || reason.isBlank()) {
            throw new DepositException(DepositErrorCode.INVALID_REQUEST);
        }
    }
}