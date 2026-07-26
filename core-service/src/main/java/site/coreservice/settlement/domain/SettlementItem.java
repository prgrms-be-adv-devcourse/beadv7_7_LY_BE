package site.coreservice.settlement.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "settlement_item", uniqueConstraints = @UniqueConstraint(name = "ukSettlementItemOrderId", columnNames = "order_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "final_bid_price", nullable = false, precision = 19, scale = 0))
    private Money finalBidPrice;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "commission_amount", nullable = false, precision = 19, scale = 0))
    private Money commissionAmount;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "net_amount", nullable = false, precision = 19, scale = 0))
    private Money netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "settlement_batch_id")
    private Long settlementBatchId;

    private SettlementItem(Long orderId, Long sellerId, Money finalBidPrice, BigDecimal commissionRate,
            LocalDateTime completedAt) {
        validate(orderId, sellerId, finalBidPrice, commissionRate, completedAt);
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.finalBidPrice = finalBidPrice;
        this.commissionRate = commissionRate;
        this.commissionAmount = finalBidPrice.multiply(commissionRate);
        this.netAmount = finalBidPrice.subtract(this.commissionAmount);
        this.status = SettlementStatus.PENDING;
        this.completedAt = completedAt;
    }

    public static SettlementItem of(Long orderId, Long sellerId, Money finalBidPrice, BigDecimal commissionRate,
            LocalDateTime completedAt) {
        return new SettlementItem(orderId, sellerId, finalBidPrice, commissionRate, completedAt);
    }

    private static void validate(Long orderId, Long sellerId, Money finalBidPrice, BigDecimal commissionRate,
            LocalDateTime completedAt) {
        Objects.requireNonNull(orderId, "orderId는 null일 수 없습니다.");
        Objects.requireNonNull(sellerId, "sellerId는 null일 수 없습니다.");
        Objects.requireNonNull(finalBidPrice, "finalBidPrice는 null일 수 없습니다.");
        Objects.requireNonNull(commissionRate, "commissionRate는 null일 수 없습니다.");
        Objects.requireNonNull(completedAt, "completedAt은 null일 수 없습니다.");
    }

    public void markPaid(Long settlementBatchId, LocalDateTime paidAt) {
        Objects.requireNonNull(settlementBatchId, "settlementBatchId는 null일 수 없습니다.");
        Objects.requireNonNull(paidAt, "paidAt은 null일 수 없습니다.");
        if (status != SettlementStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 정산 항목만 지급 처리할 수 있습니다.");
        }
        this.status = SettlementStatus.PAID;
        this.settlementBatchId = settlementBatchId;
        this.paidAt = paidAt;
    }
}
