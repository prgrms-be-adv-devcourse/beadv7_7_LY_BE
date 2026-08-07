package site.pointwalletservice.deposit.presentation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.pointwalletservice.deposit.application.DepositRequestResult;
import site.pointwalletservice.deposit.application.DepositService;
import site.pointwalletservice.deposit.exception.DepositErrorCode;
import site.pointwalletservice.deposit.presentation.dto.DepositCancelRequest;
import site.pointwalletservice.deposit.presentation.dto.DepositConfirmRequest;
import site.pointwalletservice.deposit.presentation.dto.DepositRequestRequest;
import site.pointwalletservice.deposit.presentation.dto.DepositRequestResponse;
import site.pointwalletservice.shared.Money;

@RestController
@RequestMapping("/api/v1/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;

    @PostMapping
    public ResponseEntity<ApiResponse<DepositRequestResponse>> requestDeposit(
            @RequestHeader("X-Member-Id") Long userId,
            @RequestBody DepositRequestRequest request
    ) {
        DepositRequestResult result = depositService.requestDeposit(userId, Money.of(request.amount()));
        return ResponseEntity.ok(ApiResponse.success(new DepositRequestResponse(result.orderId(), result.amount().getValue())));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmDeposit(@RequestBody DepositConfirmRequest request) {
        depositService.confirmDeposit(request.paymentKey(), request.orderId(), Money.of(request.amount()));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{depositId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelDeposit(
            @PathVariable Long depositId,
            @RequestBody DepositCancelRequest request
    ) {
        depositService.cancelDeposit(depositId, request.reason());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(IllegalArgumentException e) {
        DepositErrorCode errorCode = DepositErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }
}