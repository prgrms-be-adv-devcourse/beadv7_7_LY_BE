package site.pointwalletservice.deposit.reconciliation.presentation.dto;
import java.time.LocalDateTime;
import site.pointwalletservice.deposit.reconciliation.domain.DepositReconciliationLog;
import site.pointwalletservice.deposit.reconciliation.domain.ReconciliationFailureType;
import site.pointwalletservice.deposit.reconciliation.domain.ReconciliationLogStatus;

public record DepositReconciliationLogResponse(
        Long id,
        Long depositId,
        ReconciliationFailureType failureType,
        String providerTxId,
        String causeMessage,
        String pgSnapshot,
        ReconciliationLogStatus status,
        String resolvedNote,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static DepositReconciliationLogResponse from(DepositReconciliationLog log) {
        return new DepositReconciliationLogResponse(
                log.getId(), log.getDepositId(), log.getFailureType(), log.getProviderTxId(),
                log.getCauseMessage(), log.getPgSnapshot(), log.getStatus(), log.getResolvedNote(),
                log.getCreatedAt(), log.getResolvedAt()
        );
    }
}