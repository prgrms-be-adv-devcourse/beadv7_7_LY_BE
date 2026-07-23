package site.coreservice.pointwallet.ledger.domain;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.coreservice.pointwallet.shared.Money;

/**
 * 예치금 잔액 변동 사실 1건을 기록하는 불변 원장(append-only). 수정/삭제 메서드가 없다 —
 * 확정된 사실만 쌓이고 잔액은 이 로그를 근거로 계산된다는 원칙을 엔티티 레벨에서 강제한다.
 * <p>
 * occurredAt이 도메인 고유 시간 필드라 Deposit과 같은 이유로 BaseEntity(createdAt과 중복)를 안 쓴다.
 */
@Entity
@Table(name = "point_transaction")
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