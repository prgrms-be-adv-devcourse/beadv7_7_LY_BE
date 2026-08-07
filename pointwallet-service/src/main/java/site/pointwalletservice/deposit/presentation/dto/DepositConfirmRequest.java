package site.pointwalletservice.deposit.presentation.dto;

import java.math.BigDecimal;

public record DepositConfirmRequest(String paymentKey, String orderId, BigDecimal amount) {

    public DepositConfirmRequest {
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("paymentKey는 필수입니다.");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId는 필수입니다.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount는 필수입니다.");
        }
    }
}