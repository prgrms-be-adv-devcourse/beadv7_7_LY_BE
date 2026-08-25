package site.pointwalletservice.wallet.deadletter.domain;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * WithdrawFeeEarnedEvent가 KafkaErrorHandlerConfig의 재시도(1초 간격 3회)를 다 소진하고
 * DLT 토픽으로 격리된 것을 사람이 확인/재처리할 수 있게 남기는 기록.
 * <p>
 * DepositReconciliationLog와 같은 원칙("돈이 걸린 실패는 로그가 아니라 DB에 남겨 관리자가
 * 처리한다")을 따르되, 재처리 방법은 다르다 - reconciliation은 이미 발생한 이중 실패를 사람이
 * 수동으로 정리하는 것으로 끝나지만, 이건 원래 이벤트(withdrawId, feeAmount)를 그대로 들고
 * 있어서 관리자가 승인하면 WithdrawFeeEarnedEventHandler.handle()을 그대로 재호출해
 * 정상 처리를 재시도할 수 있다.
 */
@Entity
@Table(name = "withdraw_fee_dead_letter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawFeeDeadLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "withdraw_id", nullable = false)
    private Long withdrawId;

    @Column(name = "fee_amount", nullable = false)
    private BigDecimal feeAmount;

    /** DLT 레코드 헤더(kafka_dlt-exception-message)에서 뽑은 마지막 실패 원인. */
    @Column(name = "cause_message")
    private String causeMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeadLetterStatus status;

    /** 관리자가 resolve() 처리할 때 남기는 조치 내용(수동 처리 사유, 재처리 결과 등). */
    @Column(name = "resolved_note")
    private String resolvedNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    private WithdrawFeeDeadLetter(Long withdrawId, BigDecimal feeAmount, String causeMessage) {
        this.withdrawId = withdrawId;
        this.feeAmount = feeAmount;
        this.causeMessage = causeMessage;
        this.status = DeadLetterStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public static WithdrawFeeDeadLetter open(Long withdrawId, BigDecimal feeAmount, String causeMessage) {
        return new WithdrawFeeDeadLetter(withdrawId, feeAmount, causeMessage);
    }

    public void resolve(String note) {
        this.status = DeadLetterStatus.RESOLVED;
        this.resolvedNote = note;
        this.resolvedAt = LocalDateTime.now();
    }
}