package site.pointwalletservice.deposit.reconciliation.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "PG는 이미 처리됐는데 우리 쪽 보정까지 실패한" 이중 실패를 사람이 확인할 수 있게 남기는 기록.
 * 자동으로 재시도하거나 스스로 상태를 맞추지 않는다 - 이중 실패는 발생 빈도가 낮고, 돈이 걸린 상태를
 * 자동으로 고치는 건 실무에서도 지양하는 편이라 "일단 눈에 보이게 남기고, 사람이 처리한다"까지만
 * 다룬다. 자동 정합(주기적으로 PG 전체와 DB를 비교하는 배치)은 의도적으로 범위에서 뺐다 -
 * 이 로그가 생기는 경로(각 실패 catch 블록) 자체가 이미 실패 시점의 PG 상태 조회까지 포함하고
 * 있어서, 배치를 추가로 돌 필요 없이 실패한 건에 대한 정보는 이미 다 여기 있다.
 */
@Entity
@Table(name = "deposit_reconciliation_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositReconciliationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deposit_id", nullable = false)
    private Long depositId;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", nullable = false)
    private ReconciliationFailureType failureType;

    @Column(name = "provider_tx_id")
    private String providerTxId;

    /** 실패 원인 예외 메시지. */
    @Column(name = "cause_message")
    private String causeMessage;

    /** 실패 시점에 PaymentGatewayClient.inquire()로 조회한 PG측 실제 상태 스냅샷(사람이 읽는 요약문). */
    @Lob
    @Column(name = "pg_snapshot")
    private String pgSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationLogStatus status;

    /** 관리자가 resolve() 처리할 때 남기는 조치 내용. */
    @Column(name = "resolved_note")
    private String resolvedNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    private DepositReconciliationLog(Long depositId, ReconciliationFailureType failureType,
                                     String providerTxId, String causeMessage, String pgSnapshot) {
        this.depositId = depositId;
        this.failureType = failureType;
        this.providerTxId = providerTxId;
        this.causeMessage = causeMessage;
        this.pgSnapshot = pgSnapshot;
        this.status = ReconciliationLogStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public static DepositReconciliationLog open(Long depositId, ReconciliationFailureType failureType,
                                                String providerTxId, String causeMessage, String pgSnapshot) {
        return new DepositReconciliationLog(depositId, failureType, providerTxId, causeMessage, pgSnapshot);
    }

    public void resolve(String note) {
        this.status = ReconciliationLogStatus.RESOLVED;
        this.resolvedNote = note;
        this.resolvedAt = LocalDateTime.now();
    }
}