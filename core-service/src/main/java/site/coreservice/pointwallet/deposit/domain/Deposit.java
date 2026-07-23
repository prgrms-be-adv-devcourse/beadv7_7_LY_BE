package site.coreservice.pointwallet.deposit.domain;


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
import site.coreservice.pointwallet.deposit.exception.DepositErrorCode;
import site.coreservice.pointwallet.deposit.exception.DepositException;
import site.coreservice.pointwallet.shared.Money;

/**
 * 예치금 충전(결제) 요청 한 건을 표현하는 애그리거트 루트.
 * 애그리거트 경계 = 이 엔티티 + 내부 Money(requestedAmount) 값 객체뿐.
 * 거래내역(포인트원장)·예치금(지갑)은 별개 애그리거트이며, 여기서 id로만 참조된다(객체 참조 금지).
 */
@Entity
@Table(
        name = "deposit",
        uniqueConstraints = @UniqueConstraint(name = "ukDepositOrderId", columnNames = "order_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private String orderId;

    @Column(name = "payment_key")
    private String paymentKey;

    @Embedded
    private Money requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DepositStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    private Deposit(Long userId, String orderId, Money requestedAmount) {
        this.userId = userId;
        this.orderId = orderId;
        this.requestedAmount = requestedAmount;
        this.status = DepositStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public static Deposit request(Long userId, String orderId, Money requestedAmount) {
        return new Deposit(userId, orderId, requestedAmount);
    }

    public void confirm(String paymentKey, String orderId, Money approvedAmount) {
        validateStatus(DepositStatus.REQUESTED);
        validateRequestConsistency(orderId, approvedAmount);

        this.paymentKey = paymentKey;
        this.status = DepositStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail() {
        validateStatus(DepositStatus.REQUESTED);
        this.status = DepositStatus.FAILED;
    }

    private void validateStatus(DepositStatus expected) {
        if (this.status != expected) {
            throw new DepositException(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }
    }

    private void validateRequestConsistency(String orderId, Money approvedAmount) {
        if (!this.orderId.equals(orderId)) {
            throw new DepositException(DepositErrorCode.ORDER_ID_MISMATCH);
        }
        if (!this.requestedAmount.equals(approvedAmount)) {
            throw new DepositException(DepositErrorCode.AMOUNT_MISMATCH);
        }
    }
}