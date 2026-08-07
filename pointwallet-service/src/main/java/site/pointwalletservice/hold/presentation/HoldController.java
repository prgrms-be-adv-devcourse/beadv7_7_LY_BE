package site.pointwalletservice.hold.presentation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.pointwalletservice.hold.application.HoldResult;
import site.pointwalletservice.hold.application.HoldService;
import site.pointwalletservice.hold.exception.HoldErrorCode;
import site.pointwalletservice.hold.presentation.dto.HoldRequest;
import site.pointwalletservice.hold.presentation.dto.HoldResponse;
import site.pointwalletservice.shared.Money;

/**
 * 경매 서비스 전용 내부 API. 신규 홀드/홀드 교체를 하나의 엔드포인트로 처리한다 —
 * 경매(auctionId) 기준 기존 활성 홀드 유무 판단은 서버(HoldApplicationService)가 담당하고,
 * 클라이언트는 매번 동일한 요청 형태({auctionId, memberId, amount})만 보내면 된다.
 */
@RestController
@RequestMapping("/internal/v1/wallet")
@RequiredArgsConstructor
public class HoldController {

    private final HoldService holdService;

    @PutMapping("/hold")
    public ResponseEntity<ApiResponse<HoldResponse>> hold(@RequestBody HoldRequest request) {
        HoldResult result = holdService.hold(request.auctionId(), request.memberId(), Money.of(request.amount()));
        return ResponseEntity.ok(ApiResponse.success(
                new HoldResponse(result.holdId(), result.releasedHoldId(), result.balanceAfter().getValue())
        ));
    }

    // record 컴팩트 생성자의 입력값 검증 실패(IllegalArgumentException)만 여기서 자체 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(IllegalArgumentException e) {
        HoldErrorCode errorCode = HoldErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }
}