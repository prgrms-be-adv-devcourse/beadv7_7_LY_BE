package site.coreservice.settlement.domain;

import jakarta.persistence.AttributeOverride;
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

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "settlement_batch", uniqueConstraints = @UniqueConstraint(
        name = "ukSettlementBatchSellerPeriod", columnNames = {"seller_id", "period_from", "period_to"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "total_amount", nullable = false, precision = 19, scale = 0))
    private Money totalAmount;

    @Column(name = "period_from", nullable = false)
    private LocalDateTime periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDateTime periodTo;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    private SettlementBatch(Long sellerId, Money totalAmount, LocalDateTime periodFrom, LocalDateTime periodTo,
            LocalDateTime confirmedAt) {
        validate(sellerId, totalAmount, periodFrom, periodTo, confirmedAt);
        this.sellerId = sellerId;
        this.totalAmount = totalAmount;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.confirmedAt = confirmedAt;
    }

    public static SettlementBatch of(Long sellerId, Money totalAmount, LocalDateTime periodFrom,
            LocalDateTime periodTo, LocalDateTime confirmedAt) {
        return new SettlementBatch(sellerId, totalAmount, periodFrom, periodTo, confirmedAt);
    }

    private static void validate(Long sellerId, Money totalAmount, LocalDateTime periodFrom,
            LocalDateTime periodTo, LocalDateTime confirmedAt) {
        Objects.requireNonNull(sellerId, "sellerId는 null일 수 없습니다.");
        Objects.requireNonNull(totalAmount, "totalAmount는 null일 수 없습니다.");
        Objects.requireNonNull(periodFrom, "periodFrom은 null일 수 없습니다.");
        Objects.requireNonNull(periodTo, "periodTo는 null일 수 없습니다.");
        Objects.requireNonNull(confirmedAt, "confirmedAt은 null일 수 없습니다.");
        if (!periodFrom.isBefore(periodTo)) {
            throw new IllegalArgumentException("periodFrom은 periodTo보다 이전이어야 합니다.");
        }
    }
}
