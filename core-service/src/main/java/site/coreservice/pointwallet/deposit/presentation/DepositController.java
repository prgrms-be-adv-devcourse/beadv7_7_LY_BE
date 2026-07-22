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
import site.coreservice.pointwallet.deposit.exception.DepositErrorCode;
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
            @RequestHeader("X-User-Id") Long userId,
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

    // DepositException 핸들러는 삭제 — common의 GlobalExceptionHandler가 BusinessException을 잡아서 처리함

    // record 컴팩트 생성자의 입력값 검증 실패(IllegalArgumentException)만 여기서 자체 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(IllegalArgumentException e) {
        DepositErrorCode errorCode = DepositErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }
}