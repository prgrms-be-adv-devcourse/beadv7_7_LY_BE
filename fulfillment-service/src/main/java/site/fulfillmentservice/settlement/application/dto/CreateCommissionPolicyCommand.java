package site.fulfillmentservice.settlement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCommissionPolicyCommand(
        BigDecimal commissionRate,
        LocalDate effectiveFromDate
) {
}
