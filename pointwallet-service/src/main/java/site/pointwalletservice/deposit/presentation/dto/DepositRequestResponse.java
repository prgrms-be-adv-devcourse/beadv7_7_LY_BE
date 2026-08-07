package site.pointwalletservice.deposit.presentation.dto;

import java.math.BigDecimal;

public record DepositRequestResponse(String orderId, BigDecimal amount) {}