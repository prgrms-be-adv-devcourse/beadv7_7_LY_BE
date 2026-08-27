package site.pointwalletservice.withdraw.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;

@Entity
@Table(
        name = "withdraw_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_withdraw_idempotency_key", columnNames = "idempotency_key")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Withdraw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Embedded
    private Money amount;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "fee_amount"))
    private Money feeAmount;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "net_amount"))
    private Money netAmount;

    /**
     * 클라이언트가 요청마다(재시도 시에도 동일하게) 생성해서 헤더로 보내는 멱등키. 같은 키로 두 번
     * 이상 요청이 들어와도(버튼 중복 클릭, 네트워크 재시도 등) 지갑 차감이 한 번만 일어나도록 보장하는
     * 유일한 안전망이다 - 유니크 제약이 최종 방어선이고, 애플리케이션 레이어의 사전 조회는 그 앞단에서
     * 불필요한 재검증/재계산을 걸러내는 최적화일 뿐이다.
     */
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WithdrawStatus status;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private Withdraw(Long userId, Money amount, Money feeAmount, Money netAmount, String idempotencyKey) {
        this.userId = userId;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.idempotencyKey = idempotencyKey;
        this.status = WithdrawStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public static Withdraw request(Long userId, Money amount, Money feeAmount, Money netAmount, String idempotencyKey) {
        return new Withdraw(userId, amount, feeAmount, netAmount, idempotencyKey);
    }

    public void complete() {
        validateStatus(WithdrawStatus.PENDING);
        this.status = WithdrawStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        validateStatus(WithdrawStatus.PENDING);
        this.status = WithdrawStatus.FAILED;
        this.failReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    private void validateStatus(WithdrawStatus expected) {
        if (this.status != expected) {
            throw new WithdrawException(WithdrawErrorCode.ALREADY_PROCESSED);
        }
    }
}