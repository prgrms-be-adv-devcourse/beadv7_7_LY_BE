// outbox/presentation/OutboxAdminController.java
package site.pointwalletservice.outbox.presentation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.pointwalletservice.outbox.application.OutboxAdminService;
import site.pointwalletservice.outbox.presentation.dto.OutboxEventResponse;

/**
 * DEAD 상태 아웃박스 이벤트를 조회하고 수동으로 재시도(PENDING으로 되돌림)시키는 최소 관리자 기능.
 * deposit/reconciliation 쪽 관리자 컨트롤러와 마찬가지로 인증/권한은 이 컨트롤러 자체엔 없다 -
 * 내부 API(/internal/v1)라 상위 레이어에서 접근을 막는다는 전제.
 */
@RestController
@RequestMapping("/api/admin/v1/outbox-events")
@RequiredArgsConstructor
public class OutboxAdminController {

    private final OutboxAdminService outboxAdminService;

    @GetMapping("/dead")
    public ResponseEntity<ApiResponse<List<OutboxEventResponse>>> listDead() {
        List<OutboxEventResponse> responses = outboxAdminService.findDeadEvents().stream()
                .map(OutboxEventResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable Long id) {
        outboxAdminService.retryManually(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}