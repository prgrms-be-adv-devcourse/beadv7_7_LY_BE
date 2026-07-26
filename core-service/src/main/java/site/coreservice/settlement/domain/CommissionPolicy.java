package site.coreservice.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

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
        if (effectiveTo != null && !effectiveFrom.isBefore(effectiveTo)) {
            throw new IllegalArgumentException("effectiveFrom은 effectiveTo보다 이전이어야 합니다.");
        }
    }

    public boolean isEffectiveAt(LocalDateTime dateTime) {
        boolean afterStart = !dateTime.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || dateTime.isBefore(effectiveTo);
        return afterStart && beforeEnd;
    }

    public void close(LocalDateTime effectiveTo) {
        Objects.requireNonNull(effectiveTo, "effectiveTo는 null일 수 없습니다.");
        if (this.effectiveTo != null) {
            throw new IllegalStateException("이미 종료된 정책입니다.");
        }
        if (!effectiveFrom.isBefore(effectiveTo)) {
            throw new IllegalArgumentException("effectiveTo는 effectiveFrom보다 이후여야 합니다.");
        }
        this.effectiveTo = effectiveTo;
    }
}
