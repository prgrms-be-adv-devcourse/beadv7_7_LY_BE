package site.pointwalletservice.deposit.reconciliation.application;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.deposit.exception.DepositErrorCode;
import site.pointwalletservice.deposit.exception.DepositException;
import site.pointwalletservice.deposit.reconciliation.domain.DepositReconciliationLog;
import site.pointwalletservice.deposit.reconciliation.domain.DepositReconciliationLogRepository;
import site.pointwalletservice.deposit.reconciliation.domain.ReconciliationLogStatus;

/**
 * 관리자가 수동으로 확인/처리하는 용도의 최소 기능 - 목록 조회와 처리완료 표시 두 가지뿐이다.
 * 권한/역할 체계는 아직 이 서비스 전체에 없어서 여기서만 새로 만들지 않았다 - 인증은 상위(게이트웨이
 * 등)에서 내부 API 자체를 막는 걸 전제로 한다.
 */
@Service
@RequiredArgsConstructor
public class DepositReconciliationAdminService {

    private final DepositReconciliationLogRepository repository;

    @Transactional(readOnly = true)
    public List<DepositReconciliationLog> findByStatus(ReconciliationLogStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public void resolve(Long id, String note) {
        DepositReconciliationLog log = repository.findById(id)
                .orElseThrow(() -> new DepositException(DepositErrorCode.RECONCILIATION_LOG_NOT_FOUND));
        log.resolve(note);
        repository.save(log);
    }
}