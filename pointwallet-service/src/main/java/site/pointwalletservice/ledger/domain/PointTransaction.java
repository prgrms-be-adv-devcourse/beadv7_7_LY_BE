package site.pointwalletservice.ledger.domain;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.pointwalletservice.shared.Money;

/**
 * 예치금 잔액 변동 사실 1건을 기록하는 불변 원장(append-only). 수정/삭제 메서드가 없다 —
 * 확정된 사실만 쌓이고 잔액은 이 로그를 근거로 계산된다는 원칙을 엔티티 레벨에서 강제한다.
 * <p>
 * occurredAt이 도메인 고유 시간 필드라 Deposit과 같은 이유로 BaseEntity(createdAt과 중복)를 안 쓴다.
 * <p>
 * uk_point_transaction_related_id_type: 같은 근원 이벤트(related_id)에 대해 같은 type의 원장은
 * 한 번만 쌓일 수 있다는 제약. Kafka at-least-once로 같은 이벤트가 중복 전달돼 컨슈머 두 스레드가
 * 동시에 처리하는 경우를 막기 위한 최종 안전망 — 애플리케이션 레벨 existsBy 체크는 check-then-act라
 * 이 제약 없이는 레이스가 뚫릴 수 있다.
 */
@Entity
@Table(
        name = "point_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_point_transaction_related_id_type",
                columnNames = {"related_id", "type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false, updatable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private PointTransactionType type;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "amount", precision = 19, scale = 0, nullable = false, updatable = false))
    private Money amount;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "balance_after", precision = 19, scale = 0, nullable = false, updatable = false))
    private Money balanceAfter;

    /** 이 변동을 유발한 근원 애그리거트의 id (예: Deposit.id). 논리 참조, FK 없음. */
    @Column(name = "related_id", nullable = false, updatable = false)
    private Long relatedId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    private PointTransaction(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.relatedId = relatedId;
        this.occurredAt = LocalDateTime.now();
    }

    public static PointTransaction record(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId) {
        return new PointTransaction(walletId, type, amount, balanceAfter, relatedId);
    }
}