package site.coreservice.pointwallet.deposit.infrastructure.toss;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.coreservice.pointwallet.deposit.domain.PaymentGatewayClient;
import site.coreservice.pointwallet.deposit.domain.PgApproveResult;
import site.coreservice.pointwallet.deposit.domain.PgCancelResult;
import site.coreservice.pointwallet.shared.Money;

@Component
@RequiredArgsConstructor
public class TossPaymentGatewayAdapter implements PaymentGatewayClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String CANCEL_URL = "https://api.tosspayments.com/v1/payments/%s/cancel";

    private final RestClient restClient;
    private final TossPaymentsProperties properties;

    @Override
    public PgApproveResult approve(String providerTxId, String orderId, Money amount) {
        TossConfirmApiResponse response = restClient.post()
                .uri(CONFIRM_URL)
                .header("Authorization", basicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "paymentKey", providerTxId,
                        "orderId", orderId,
                        "amount", amount.getValue()
                ))
                .exchange((request, response1) -> {
                    if (!response1.getStatusCode().is2xxSuccessful()) {
                        throw toApiException(response1.getStatusCode(), response1);
                    }
                    return response1.bodyTo(TossConfirmApiResponse.class);
                });

        return new PgApproveResult(
                response.paymentKey(),
                response.orderId(),
                Money.of(response.totalAmount())
        );
    }

    @Override
    public PgCancelResult cancel(String providerTxId, String cancelReason, Money cancelAmount) {
        TossCancelApiResponse response = restClient.post()
                .uri(CANCEL_URL.formatted(providerTxId))
                .header("Authorization", basicAuthHeader())
                .header("Idempotency-Key", "DEPOSIT-CANCEL-" + providerTxId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "cancelReason", cancelReason,
                        "cancelAmount", cancelAmount.getValue()
                ))
                .exchange((request, response1) -> {
                    if (!response1.getStatusCode().is2xxSuccessful()) {
                        throw toApiException(response1.getStatusCode(), response1);
                    }
                    return response1.bodyTo(TossCancelApiResponse.class);
                });

        TossCancelApiResponse.CancelDetail latestCancel = response.cancels()
                .get(response.cancels().size() - 1);

        return new PgCancelResult(
                response.paymentKey(),
                latestCancel.transactionKey(),
                Money.of(latestCancel.cancelAmount())
        );
    }

    private TossPaymentsApiException toApiException(
            HttpStatusCode status,
            RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response
    ) {
        String body;
        try {
            body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            body = "(응답 본문 읽기 실패)";
        }
        return new TossPaymentsApiException(status, body);
    }

    private String basicAuthHeader() {
        String credentials = properties.secretKey() + ":";
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}