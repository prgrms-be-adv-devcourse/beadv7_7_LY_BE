// deposit/reconciliation/domain/DepositReconciliationLogRepository.java
package site.pointwalletservice.deposit.reconciliation.domain;
import java.util.List;
import java.util.Optional;

public interface DepositReconciliationLogRepository {

    DepositReconciliationLog save(DepositReconciliationLog log);

    Optional<DepositReconciliationLog> findById(Long id);

    /** 관리자 목록 조회용 - 최신순. */
    List<DepositReconciliationLog> findByStatusOrderByCreatedAtDesc(ReconciliationLogStatus status);
}