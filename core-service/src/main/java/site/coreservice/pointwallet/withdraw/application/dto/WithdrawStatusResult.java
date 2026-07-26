package site.coreservice.pointwallet.withdraw.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.coreservice.pointwallet.withdraw.domain.Withdraw;
import site.coreservice.pointwallet.withdraw.domain.WithdrawStatus;

public record WithdrawStatusResult(Long withdrawRequestId, WithdrawStatus status, BigDecimal amount,
                                   String failReason, LocalDateTime requestedAt, LocalDateTime processedAt) {

    public static WithdrawStatusResult from(Withdraw withdraw) {
        return new WithdrawStatusResult(withdraw.getId(), withdraw.getStatus(),
                withdraw.getAmount().getValue(), withdraw.getFailReason(),
                withdraw.getRequestedAt(), withdraw.getProcessedAt());
    }
}