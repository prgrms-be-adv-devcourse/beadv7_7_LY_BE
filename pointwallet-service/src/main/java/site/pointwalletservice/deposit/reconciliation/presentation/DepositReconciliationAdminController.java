// deposit/reconciliation/presentation/DepositReconciliationAdminController.java
package site.pointwalletservice.deposit.reconciliation.presentation;
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
import site.pointwalletservice.deposit.reconciliation.application.DepositReconciliationAdminService;
import site.pointwalletservice.deposit.reconciliation.domain.ReconciliationLogStatus;
import site.pointwalletservice.deposit.reconciliation.presentation.dto.DepositReconciliationLogResponse;
import site.pointwalletservice.deposit.reconciliation.presentation.dto.ResolveReconciliationLogRequest;

/**
 * PG-DB 정합성이 깨진 채로 남은 건을 사람이 조회/처리하는 최소 관리자 기능. 인증/권한은 이
 * 컨트롤러 자체엔 없다 - 내부 API(/internal/v1)라 게이트웨이 등 상위 레이어에서 접근을 막는다는
 * 전제. 관리자 인증 체계를 여기서 새로 만드는 건 지금 스코프를 벗어난다고 판단했다.
 */
@RestController
@RequestMapping("/internal/v1/admin/deposit-reconciliation-logs")
@RequiredArgsConstructor
public class DepositReconciliationAdminController {

    private final DepositReconciliationAdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepositReconciliationLogResponse>>> list(
            @RequestParam(defaultValue = "OPEN") ReconciliationLogStatus status
    ) {
        List<DepositReconciliationLogResponse> responses = adminService.findByStatus(status).stream()
                .map(DepositReconciliationLogResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolve(
            @PathVariable Long id,
            @RequestBody ResolveReconciliationLogRequest request
    ) {
        adminService.resolve(id, request.note());
        return ResponseEntity.ok(ApiResponse.success());
    }
}