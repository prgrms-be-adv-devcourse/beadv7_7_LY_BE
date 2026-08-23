package site.pointwalletservice.hold.presentation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import site.pointwalletservice.hold.presentation.dto.HoldRollbackRequest;
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

    /**
     * hold() 성공 이후 호출자(auction-service)의 자기 트랜잭션이 실패했을 때 부르는 보상 API.
     * hold() 때 보냈던 요청을 그대로 다시 실어 보내면 된다({auctionId, memberId, amount}) — holdId는
     * PathVariable로 받되, 요청 바디 값이 서버가 원장·Hold로 재구성한 실제 값과 하나라도 다르면
     * HOLD_MISMATCH로 거부한다(추측으로 다른 홀드를 맞춰서 처리하지 않음). 이미 교체/소멸돼서
     * 되돌릴 게 없으면 조용히 성공 처리(멱등)한다.
     */
    @PostMapping("/hold/{holdId}/rollback")
    public ResponseEntity<ApiResponse<Void>> rollback(@PathVariable Long holdId,
                                                      @RequestBody HoldRollbackRequest request) {
        holdService.rollback(holdId, request.auctionId(), request.memberId(), Money.of(request.amount()));
        return ResponseEntity.ok(ApiResponse.success());
    }

    // record 컴팩트 생성자의 입력값 검증 실패(IllegalArgumentException)만 여기서 자체 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(IllegalArgumentException e) {
        HoldErrorCode errorCode = HoldErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }
}