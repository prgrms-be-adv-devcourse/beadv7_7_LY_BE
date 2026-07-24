package site.coreservice.pointwallet.deposit.presentation.dto;
import site.coreservice.pointwallet.deposit.exception.DepositErrorCode;
import site.coreservice.pointwallet.deposit.exception.DepositException;

public record DepositCancelRequest(String reason) {

    public DepositCancelRequest {
        if (reason == null || reason.isBlank()) {
            throw new DepositException(DepositErrorCode.INVALID_REQUEST);
        }
    }
}