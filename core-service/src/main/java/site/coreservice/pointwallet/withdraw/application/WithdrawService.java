package site.coreservice.pointwallet.withdraw.application;

import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.withdraw.application.dto.WithdrawRequestResult;
import site.coreservice.pointwallet.withdraw.application.dto.WithdrawStatusResult;

public interface WithdrawService {
    WithdrawRequestResult requestWithdraw(Long userId, Money amount);
    WithdrawStatusResult getStatus(Long withdrawRequestId);
}