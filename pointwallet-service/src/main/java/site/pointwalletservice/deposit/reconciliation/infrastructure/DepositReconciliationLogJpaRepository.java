// deposit/reconciliation/infrastructure/DepositReconciliationLogJpaRepository.java
package site.pointwalletservice.deposit.reconciliation.infrastructure;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.pointwalletservice.deposit.reconciliation.domain.DepositReconciliationLog;
import site.pointwalletservice.deposit.reconciliation.domain.ReconciliationLogStatus;

public interface DepositReconciliationLogJpaRepository extends JpaRepository<DepositReconciliationLog, Long> {
    List<DepositReconciliationLog> findByStatusOrderByCreatedAtDesc(ReconciliationLogStatus status);
}