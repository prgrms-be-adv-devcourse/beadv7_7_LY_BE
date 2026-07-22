package site.coreservice.pointwallet.deposit.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.coreservice.pointwallet.deposit.application.DepositRequestResult;
import site.coreservice.pointwallet.deposit.application.DepositService;
import site.coreservice.pointwallet.deposit.domain.DepositErrorCode;
import site.coreservice.pointwallet.deposit.domain.DepositException;
import site.coreservice.pointwallet.deposit.presentation.dto.DepositConfirmRequest;
import site.coreservice.pointwallet.deposit.presentation.dto.DepositRequestRequest;
import site.coreservice.pointwallet.deposit.presentation.dto.DepositRequestResponse;
import site.coreservice.pointwallet.shared.Money;

@RestController
@RequestMapping("/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;

    @PostMapping
    public ApiResponse<DepositRequestResponse> requestDeposit(
            @RequestHeader("X-User-Id") Long userId, // TODO: 인증 방식 확정되면 교체
            @RequestBody DepositRequestRequest request
    ) {
        DepositRequestResult result = depositService.requestDeposit(userId, Money.of(request.amount()));
        return ApiResponse.success(new DepositRequestResponse(result.orderId(), result.amount().getValue()));
    }

    @PostMapping("/confirm")
    public ApiResponse<Void> confirmDeposit(@RequestBody DepositConfirmRequest request) {
        depositService.confirmDeposit(request.paymentKey(), request.orderId(), Money.of(request.amount()));
        return ApiResponse.success();
    }

    // TODO: PR #50(common BusinessException) 머지되면 삭제.
    @ExceptionHandler(DepositException.class)
    public ResponseEntity<ApiResponse<Void>> handleDepositException(DepositException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getErrorCode()));
    }

    // record 컴팩트 생성자에서 던지는 입력값 검증 실패를 400으로 매핑
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(DepositErrorCode.INVALID_REQUEST));
    }
}