package site.fulfillmentservice.settlement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;

public record CommissionPolicyResult(
        Long commissionPolicyId,
        BigDecimal commissionRate,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo
) {

    public static CommissionPolicyResult from(CommissionPolicy commissionPolicy) {
        return new CommissionPolicyResult(
                commissionPolicy.getId(),
                commissionPolicy.getCommissionRate(),
                commissionPolicy.getEffectiveFrom(),
                commissionPolicy.getEffectiveTo()
        );
    }
}
