package site.coreservice.pointwallet.withdraw.presentation.dto;
import java.math.BigDecimal;

public record WithdrawRequest(BigDecimal amount) {
}