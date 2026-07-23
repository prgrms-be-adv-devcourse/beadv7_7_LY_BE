package site.coreservice.pointwallet.hold.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.coreservice.pointwallet.shared.Money;

/**
 * 경매 입찰 시 홀딩된 예치금 1건을 표현하는 애그리거트 루트.
 * 애그리거트 경계 = 이 엔티티 + 내부 Money(amount) 값 객체뿐.
 * <p>
 * 상태 필드가 없는 완전 불변 객체다 — "이 경매에 활성 홀드가 있는가"는 auction_id로 레코드가
 * 존재하는지로 판단하고(유니크 제약으로 경매당 최대 1건), 해제·정산 시점에는 레코드 자체를 delete한다.
 * 홀드/해제 히스토리는 PointTransaction(포인트원장)이 별도로 전담해서 기록한다.
 */
@Entity
@Table(
        name = "hold",
        uniqueConstraints = @UniqueConstraint(name = "ukHoldAuctionId", columnNames = "auction_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "auction_id", nullable = false, updatable = false)
    private Long auctionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Embedded
    private Money amount;

    @Column(name = "held_at", nullable = false, updatable = false)
    private LocalDateTime heldAt;

    private Hold(Long auctionId, Long userId, Money amount) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
        this.heldAt = LocalDateTime.now();
    }

    public static Hold place(Long auctionId, Long userId, Money amount) {
        return new Hold(auctionId, userId, amount);
    }
}