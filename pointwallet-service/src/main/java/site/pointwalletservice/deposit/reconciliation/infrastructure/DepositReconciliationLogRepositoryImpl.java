// deposit/reconciliation/infrastructure/DepositReconciliationLogRepositoryImpl.java
package site.pointwalletservice.deposit.reconciliation.infrastructure;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.pointwalletservice.deposit.reconciliation.domain.DepositReconciliationLog;
import site.pointwalletservice.deposit.reconciliation.domain.DepositReconciliationLogRepository;
import site.pointwalletservice.deposit.reconciliation.domain.ReconciliationLogStatus;

@Repository
@RequiredArgsConstructor
public class DepositReconciliationLogRepositoryImpl implements DepositReconciliationLogRepository {

    private final DepositReconciliationLogJpaRepository jpaRepository;

    @Override
    public DepositReconciliationLog save(DepositReconciliationLog log) {
        return jpaRepository.save(log);
    }

    @Override
    public Optional<DepositReconciliationLog> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<DepositReconciliationLog> findByStatusOrderByCreatedAtDesc(ReconciliationLogStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}