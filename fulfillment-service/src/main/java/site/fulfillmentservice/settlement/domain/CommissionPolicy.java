package site.fulfillmentservice.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;
import site.fulfillmentservice.settlement.exception.SettlementErrorCode;
import site.fulfillmentservice.settlement.exception.SettlementException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "commission_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommissionPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    private CommissionPolicy(BigDecimal commissionRate, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        validate(commissionRate, effectiveFrom, effectiveTo);
        this.commissionRate = commissionRate;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public static CommissionPolicy of(BigDecimal commissionRate, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        return new CommissionPolicy(commissionRate, effectiveFrom, effectiveTo);
    }

    private static void validate(BigDecimal commissionRate, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        Objects.requireNonNull(commissionRate, "commissionRate는 null일 수 없습니다.");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom은 null일 수 없습니다.");
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new SettlementException(SettlementErrorCode.INVALID_COMMISSION_RATE);
        }
        if (effectiveTo != null && !effectiveFrom.isBefore(effectiveTo)) {
            throw new SettlementException(SettlementErrorCode.INVALID_EFFECTIVE_PERIOD);
        }
    }

    public boolean isEffectiveAt(LocalDateTime dateTime) {
        boolean afterStart = !dateTime.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || dateTime.isBefore(effectiveTo);
        return afterStart && beforeEnd;
    }

    public boolean isPending(LocalDateTime dateTime) {
        return effectiveFrom.isAfter(dateTime);
    }

    public void close(LocalDateTime effectiveTo) {
        Objects.requireNonNull(effectiveTo, "effectiveTo는 null일 수 없습니다.");
        if (this.effectiveTo != null) {
            throw new SettlementException(SettlementErrorCode.COMMISSION_POLICY_ALREADY_CLOSED);
        }
        if (!effectiveFrom.isBefore(effectiveTo)) {
            throw new SettlementException(SettlementErrorCode.INVALID_EFFECTIVE_PERIOD);
        }
        this.effectiveTo = effectiveTo;
    }
}
