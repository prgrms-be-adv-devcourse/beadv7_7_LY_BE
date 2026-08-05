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

@Entity
@Table(
        name = "deposit",
        uniqueConstraints = @UniqueConstraint(name = "uk_deposit_orderId", columnNames = "order_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private String orderId;

    @Column(name = "payment_key")
   private String providerTransactionId;

    @Embedded
    private Money requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DepositStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

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

    public void confirm(String providerTransactionId, String orderId, Money approvedAmount) {
        validateStatus(DepositStatus.REQUESTED);
        validateRequestConsistency(orderId, approvedAmount);

        this.providerTransactionId = providerTransactionId;
        this.status = DepositStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail() {
        validateStatus(DepositStatus.REQUESTED);
        this.status = DepositStatus.FAILED;
    }

    public void cancel(String reason) {
        validateStatus(DepositStatus.DONE);
        this.status = DepositStatus.CANCELED;
        this.cancelReason = reason;
        this.canceledAt = LocalDateTime.now();
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