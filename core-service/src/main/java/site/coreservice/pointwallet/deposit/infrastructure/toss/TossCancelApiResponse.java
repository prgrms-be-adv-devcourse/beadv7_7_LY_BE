package site.coreservice.pointwallet.deposit.infrastructure.toss;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelApiResponse(
        String paymentKey,
        List<CancelDetail> cancels
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CancelDetail(
            String transactionKey,
            BigDecimal cancelAmount,
            String cancelStatus
    ) {}
}