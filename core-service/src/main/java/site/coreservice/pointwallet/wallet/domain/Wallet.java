package site.coreservice.pointwallet.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;
import site.coreservice.pointwallet.shared.Money;

/**
 * 사용자별 예치금 잔액. 애그리거트 경계 = 자기 자신 + Money(balance) 값 객체뿐.
 * Deposit·PointTransaction은 별개 애그리거트이며 여기서 id로만 참조된다.
 * <p>
 * 도메인 고유 시간 필드가 없어서(Deposit과 달리 requestedAt 같은 게 없음) BaseEntity를 그대로 쓴다 —
 * createdAt = 지갑 개설 시각, updatedAt = 마지막 잔액 변동 시각으로 자연스럽게 의미가 겹치지 않는다.
 */
@Entity
@Table(
        name = "wallet",
        uniqueConstraints = @UniqueConstraint(name = "ukWalletUserId", columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Embedded
    private Money balance;

    private Wallet(Long userId) {
        this.userId = userId;
        this.balance = Money.zero();
    }

    public static Wallet open(Long userId) {
        return new Wallet(userId);
    }

    /** 충전 확정된 금액만큼 잔액을 늘린다. */
    public void charge(Money amount) {
        this.balance = this.balance.add(amount);
    }
}