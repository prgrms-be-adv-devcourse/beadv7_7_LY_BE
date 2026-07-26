package site.coreservice.pointwallet.withdraw.presentation.dto;

import java.math.BigDecimal;
import site.coreservice.pointwallet.withdraw.application.dto.WithdrawRequestResult;
import site.coreservice.pointwallet.withdraw.domain.WithdrawStatus;

// 문서 원안(withdrawRequestId, status)에 feeAmount/netAmount 추가 — 인출 수수료 신설에 따른 문서 갱신 필요
public record WithdrawRequestResponse(Long withdrawRequestId, WithdrawStatus status,
                                      BigDecimal feeAmount, BigDecimal netAmount) {

    public static WithdrawRequestResponse from(WithdrawRequestResult result) {
        return new WithdrawRequestResponse(result.withdrawRequestId(), result.status(),
                result.feeAmount(), result.netAmount());
    }
}