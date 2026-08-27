package site.pointwalletservice.withdraw.presentation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.WithdrawService;
import site.pointwalletservice.withdraw.domain.Withdraw;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;
import site.pointwalletservice.withdraw.presentation.dto.WithdrawRequest;
import site.pointwalletservice.withdraw.presentation.dto.WithdrawRequestResponse;
import site.pointwalletservice.withdraw.presentation.dto.WithdrawStatusResponse;

@RestController
@RequestMapping("/api/v1/wallet/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @PostMapping("/request")
    public ApiResponse<WithdrawRequestResponse> requestWithdraw(
            @MemberId Long memberId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody WithdrawRequest request) {
        Withdraw.validateIdempotencyKey(idempotencyKey);
        return ApiResponse.success(WithdrawRequestResponse.from(
                withdrawService.requestWithdraw(memberId, Money.of(request.amount()), idempotencyKey)));
    }

    @GetMapping("/{withdrawRequestId}")
    public ApiResponse<WithdrawStatusResponse> getStatus(@PathVariable Long withdrawRequestId) {
        return ApiResponse.success(WithdrawStatusResponse.from(withdrawService.getStatus(withdrawRequestId)));
    }

}