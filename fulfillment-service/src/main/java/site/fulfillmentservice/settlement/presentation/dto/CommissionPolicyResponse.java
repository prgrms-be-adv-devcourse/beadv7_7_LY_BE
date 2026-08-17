package site.fulfillmentservice.settlement.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.fulfillmentservice.settlement.application.dto.CommissionPolicyResult;

public record CommissionPolicyResponse(
        Long commissionPolicyId,
        BigDecimal commissionRate,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo
) {

    public static CommissionPolicyResponse from(CommissionPolicyResult result) {
        return new CommissionPolicyResponse(
                result.commissionPolicyId(),
                result.commissionRate(),
                result.effectiveFrom(),
                result.effectiveTo()
        );
    }
}
