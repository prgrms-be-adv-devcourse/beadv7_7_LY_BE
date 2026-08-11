package site.pointwalletservice.deposit.domain;
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
import site.pointwalletservice.deposit.exception.DepositErrorCode;
import site.pointwalletservice.deposit.exception.DepositException;
import site.pointwalletservice.shared.Money;


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

    /** PG 승인 호출 같은 비싼 외부 호출 전에, 애플리케이션 레이어가 미리 걸러낼 때 쓰는 질의 메서드.
     *  "REQUESTED 상태여야 확정 가능하다"는 규칙은 여기 한 곳에만 존재한다. */
    public boolean isConfirmable() {
        return this.status == DepositStatus.REQUESTED;
    }

    /** PG 취소 호출 전 사전 확인용. "DONE 상태여야 취소 가능하다"는 규칙도 여기 한 곳에만 존재한다. */
    public boolean isCancelable() {
        return this.status == DepositStatus.DONE;
    }

    /** 콜백으로 들어온 금액이 신청 금액과 일치하는지 확인. Money 비교 규칙을 도메인 밖으로 노출하지 않는다. */
    public boolean matchesAmount(Money amount) {
        return this.requestedAmount.equals(amount);
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