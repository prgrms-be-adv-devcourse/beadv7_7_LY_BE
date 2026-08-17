package site.fulfillmentservice.settlement.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import site.fulfillmentservice.settlement.application.dto.CreateCommissionPolicyCommand;

public record CreateCommissionPolicyRequest(
        BigDecimal commissionRate,
        LocalDate effectiveFromDate
) {

    public CreateCommissionPolicyCommand toCommand() {
        return new CreateCommissionPolicyCommand(commissionRate, effectiveFromDate);
    }
}
