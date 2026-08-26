package site.pointwalletservice.hold.domain;

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
import site.pointwalletservice.shared.Money;

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
        uniqueConstraints = @UniqueConstraint(name = "uk_hold_auction_id", columnNames = "auction_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    /**
     * 이 홀드가 (userId, amount) 요청과 실질적으로 동일한 홀드인지 판단한다 - 즉 이 요청을
     * 처리해도 최고 입찰자·홀드 금액에 아무 변화가 없는지를 홀드 스스로에게 묻는 것이다.
     * <p>
     * hold() 재시도(auction-service의 응답유실 후 재호출 등)로 이미 걸려있는 것과 같은 요청이
     * 다시 들어왔을 때, 애플리케이션 계층이 getUserId()/getAmount()를 까서 직접 비교하지 않고
     * 이 메서드를 통해 판단하게 하기 위해 존재한다 - "무엇이 같은 홀드인가"는 Hold 자신의
     * 지식이지 호출부의 절차가 아니다.
     */
    public boolean isSameRequest(Long userId, Money amount) {
        return this.userId.equals(userId) && this.amount.equals(amount);
    }
}