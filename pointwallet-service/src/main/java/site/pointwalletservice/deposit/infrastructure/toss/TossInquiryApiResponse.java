package site.pointwalletservice.deposit.infrastructure.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossInquiryApiResponse(
        String paymentKey,
        String orderId,
        BigDecimal totalAmount,
        BigDecimal balanceAmount,
        String status
) {}