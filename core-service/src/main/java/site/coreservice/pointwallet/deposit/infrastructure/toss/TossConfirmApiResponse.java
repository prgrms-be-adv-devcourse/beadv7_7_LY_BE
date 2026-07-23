package site.coreservice.pointwallet.deposit.infrastructure.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;


@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmApiResponse(
        String paymentKey,
        String orderId,
        BigDecimal totalAmount,
        String status
) {}