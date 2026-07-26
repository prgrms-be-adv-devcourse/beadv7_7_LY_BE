package site.coreservice.pointwallet.withdraw.presentation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.withdraw.application.WithdrawService;
import site.coreservice.pointwallet.withdraw.presentation.dto.WithdrawRequest;
import site.coreservice.pointwallet.withdraw.presentation.dto.WithdrawRequestResponse;
import site.coreservice.pointwallet.withdraw.presentation.dto.WithdrawStatusResponse;

@RestController
@RequestMapping("/api/v1/wallet/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @PostMapping("/request")
    public ApiResponse<WithdrawRequestResponse> requestWithdraw(
            @MemberId Long memberId, @RequestBody WithdrawRequest request) {
        return ApiResponse.success(WithdrawRequestResponse.from(
                withdrawService.requestWithdraw(memberId, Money.of(request.amount()))));
    }

    @GetMapping("/{withdrawRequestId}")
    public ApiResponse<WithdrawStatusResponse> getStatus(@PathVariable Long withdrawRequestId) {
        return ApiResponse.success(WithdrawStatusResponse.from(withdrawService.getStatus(withdrawRequestId)));
    }
}