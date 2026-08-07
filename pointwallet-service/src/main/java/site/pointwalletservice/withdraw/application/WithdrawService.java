package site.pointwalletservice.withdraw.application;

import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;

public interface WithdrawService {
    WithdrawRequestResult requestWithdraw(Long userId, Money amount);
    WithdrawStatusResult getStatus(Long withdrawRequestId);
}