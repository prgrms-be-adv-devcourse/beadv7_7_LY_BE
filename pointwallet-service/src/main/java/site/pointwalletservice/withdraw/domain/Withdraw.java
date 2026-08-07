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
@Table(name = "withdraw_request")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WithdrawStatus status;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private Withdraw(Long userId, Money amount, Money feeAmount, Money netAmount) {
        this.userId = userId;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.status = WithdrawStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public static Withdraw request(Long userId, Money amount, Money feeAmount, Money netAmount) {
        return new Withdraw(userId, amount, feeAmount, netAmount);
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