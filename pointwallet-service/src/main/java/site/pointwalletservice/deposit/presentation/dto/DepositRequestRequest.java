package site.pointwalletservice.deposit.presentation.dto;

import java.math.BigDecimal;

public record DepositRequestRequest(BigDecimal amount) {

    private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(1000);

    public DepositRequestRequest {
        if (amount == null) {
            throw new IllegalArgumentException("충전 금액은 필수입니다.");
        }
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("충전 금액은 최소 1000원 이상이어야 합니다.");
        }
    }
}