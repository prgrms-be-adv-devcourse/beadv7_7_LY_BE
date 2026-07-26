package site.coreservice.pointwallet.withdraw.application.dto;
import java.math.BigDecimal;
import site.coreservice.pointwallet.withdraw.domain.Withdraw;
import site.coreservice.pointwallet.withdraw.domain.WithdrawStatus;

public record WithdrawRequestResult(Long withdrawRequestId, WithdrawStatus status,
                                    BigDecimal feeAmount, BigDecimal netAmount) {

    public static WithdrawRequestResult from(Withdraw withdraw) {
        return new WithdrawRequestResult(withdraw.getId(), withdraw.getStatus(),
                withdraw.getFeeAmount().getValue(), withdraw.getNetAmount().getValue());
    }
}