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

    /**
     * HOLD/RELEASE 타입에 한해 채워지는 경매 id. 그 외 타입은 null.
     * <p>
     * related_id(=holdId)만으로는 Hold 로우가 삭제된 뒤(주문완료 시 consume()이 하드 딜리트) 원장이
     * 자기 자신만으로 "이 변동이 어느 경매 건이었는지"를 재구성할 수 없다 — append-only 원장이
     * 살아있는 다른 테이블에 의존해야만 과거 사실을 설명할 수 있다는 뜻이라 원장의 목적에 어긋난다.
     * 그래서 조회 전용으로 auction_id를 별도 컬럼에 남긴다. related_id는 그대로 holdId를 쓴다 —
     * 한 경매에 입찰이 여러 번 갈아치워지며 HOLD 원장이 반복 생성되므로, 유니크 제약
     * (related_id+type)을 auction_id로 바꾸면 재입찰마다 제약을 위반하게 된다.
     */
    @Column(name = "auction_id", updatable = false)
    private Long auctionId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    private PointTransaction(Long walletId, PointTransactionType type, Money amount, Money balanceAfter,
                             Long relatedId, Long auctionId) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.relatedId = relatedId;
        this.auctionId = auctionId;
        this.occurredAt = LocalDateTime.now();
    }

    public static PointTransaction record(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId) {
        return new PointTransaction(walletId, type, amount, balanceAfter, relatedId, null);
    }

    /** HOLD/RELEASE처럼 경매 원장 조회가 필요한 타입 전용 — auctionId까지 같이 남긴다. */
    public static PointTransaction recordForAuction(Long walletId, PointTransactionType type, Money amount,
                                                    Money balanceAfter, Long relatedId, Long auctionId) {
        return new PointTransaction(walletId, type, amount, balanceAfter, relatedId, auctionId);
    }
}