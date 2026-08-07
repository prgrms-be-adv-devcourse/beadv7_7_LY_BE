package site.pointwalletservice.wallet.presentation.dto;
import java.math.BigDecimal;

public record WalletBalanceResponse(BigDecimal availableBalance) {

    public static WalletBalanceResponse from(BigDecimal availableBalance) {
        return new WalletBalanceResponse(availableBalance);
    }
}