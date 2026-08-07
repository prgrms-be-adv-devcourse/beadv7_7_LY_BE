package site.pointwalletservice.deposit.infrastructure.toss;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import site.pointwalletservice.deposit.domain.PaymentGatewayClient;
import site.pointwalletservice.deposit.domain.PgApproveResult;
import site.pointwalletservice.deposit.domain.PgCancelResult;
import site.pointwalletservice.shared.Money;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentGatewayAdapter implements PaymentGatewayClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String CANCEL_URL = "https://api.tosspayments.com/v1/payments/%s/cancel";
    private static final int MAX_CONNECTION_RETRY = 2;

    private final RestClient restClient;
    private final TossPaymentsProperties properties;

    @Override
    public PgApproveResult approve(String providerTxId, String orderId, Money amount) {
        TossConfirmApiResponse response = executeWithConnectionRetry("confirm", () ->
                restClient.post()
                        .uri(CONFIRM_URL)
                        .header("Authorization", basicAuthHeader())
                        .header("Idempotency-Key", "DEPOSIT-CONFIRM-" + orderId)
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
                        })
        );

        return new PgApproveResult(
                response.paymentKey(),
                response.orderId(),
                Money.of(response.totalAmount())
        );
    }

    @Override
    public PgCancelResult cancel(String providerTxId, String cancelReason, Money cancelAmount) {
        TossCancelApiResponse response = executeWithConnectionRetry("cancel", () ->
                restClient.post()
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
                        })
        );

        TossCancelApiResponse.CancelDetail latestCancel = response.cancels()
                .get(response.cancels().size() - 1);

        return new PgCancelResult(
                response.paymentKey(),
                latestCancel.transactionKey(),
                Money.of(latestCancel.cancelAmount())
        );
    }

    /**
     * 연결 자체가 안 된 경우(타임아웃/커넥션 거부)에만 짧게 재시도한다.
     * 4xx 등 비즈니스 오류(TossPaymentsApiException)는 재시도하지 않고 그대로 전파한다.
     * Idempotency-Key를 이미 헤더에 실어 보내므로, 응답을 못 받고 재시도하더라도
     * Toss 쪽에서 같은 키에 대한 중복 처리를 막아준다.
     */
    private <T> T executeWithConnectionRetry(String operationName, java.util.function.Supplier<T> call) {
        ResourceAccessException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_CONNECTION_RETRY; attempt++) {
            try {
                return call.get();
            } catch (ResourceAccessException e) {
                lastFailure = e;
                log.warn("Toss {} 호출 연결 실패 (attempt {}/{})", operationName, attempt, MAX_CONNECTION_RETRY, e);
            }
        }
        throw lastFailure;
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