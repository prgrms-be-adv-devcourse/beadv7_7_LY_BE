package site.pointwalletservice.wallet.deadletter.presentation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.pointwalletservice.wallet.deadletter.application.WithdrawFeeDeadLetterAdminService;
import site.pointwalletservice.wallet.deadletter.domain.DeadLetterStatus;
import site.pointwalletservice.wallet.deadletter.presentation.dto.ResolveDeadLetterRequest;
import site.pointwalletservice.wallet.deadletter.presentation.dto.WithdrawFeeDeadLetterResponse;

/**
 * 인출 수수료 적립 실패(DLT 격리) 건을 사람이 조회/재처리하는 최소 관리자 기능.
 * DepositReconciliationAdminController와 동일하게 인증/권한은 이 컨트롤러 자체엔 없다 -
 * 내부 API(/internal/v1)라 게이트웨이 등 상위 레이어에서 접근을 막는다는 전제.
 */
@RestController
@RequestMapping("/api/admin/v1/withdraw-fee-dead-letters")
@RequiredArgsConstructor
public class WithdrawFeeDeadLetterAdminController {

    private final WithdrawFeeDeadLetterAdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WithdrawFeeDeadLetterResponse>>> list(
            @RequestParam(defaultValue = "OPEN") DeadLetterStatus status
    ) {
        List<WithdrawFeeDeadLetterResponse> responses = adminService.findByStatus(status).stream()
                .map(WithdrawFeeDeadLetterResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /** 원래 이벤트 처리를 그대로 재호출해 정상 적립을 재시도한다(멱등 처리라 안전). */
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<ApiResponse<Void>> reprocess(@PathVariable Long id) {
        adminService.reprocess(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 재처리 없이 사유만 남기고 확인 처리한다. */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolve(
            @PathVariable Long id,
            @RequestBody ResolveDeadLetterRequest request
    ) {
        adminService.resolve(id, request.note());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
