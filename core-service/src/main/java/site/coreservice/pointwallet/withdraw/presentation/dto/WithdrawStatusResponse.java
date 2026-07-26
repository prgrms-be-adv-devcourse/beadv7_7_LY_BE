package site.coreservice.pointwallet.withdraw.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.coreservice.pointwallet.withdraw.application.dto.WithdrawStatusResult;
import site.coreservice.pointwallet.withdraw.domain.WithdrawStatus;

public record WithdrawStatusResponse(Long withdrawRequestId, WithdrawStatus status, BigDecimal amount,
                                     String failReason, LocalDateTime requestedAt, LocalDateTime processedAt) {

    public static WithdrawStatusResponse from(WithdrawStatusResult result) {
        return new WithdrawStatusResponse(result.withdrawRequestId(), result.status(), result.amount(),
                result.failReason(), result.requestedAt(), result.processedAt());
    }
}