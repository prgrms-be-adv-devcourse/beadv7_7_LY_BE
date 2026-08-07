package site.pointwalletservice.withdraw.presentation.dto;
import java.math.BigDecimal;

public record WithdrawRequest(BigDecimal amount) {
}