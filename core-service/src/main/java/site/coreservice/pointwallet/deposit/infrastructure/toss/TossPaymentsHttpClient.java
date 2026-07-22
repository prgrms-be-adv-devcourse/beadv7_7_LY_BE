package site.coreservice.pointwallet.deposit.infrastructure.toss;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.coreservice.pointwallet.deposit.domain.TossConfirmResult;
import site.coreservice.pointwallet.deposit.domain.TossPaymentsClient;
import site.coreservice.pointwallet.shared.Money;

@Component
@RequiredArgsConstructor
public class TossPaymentsHttpClient implements TossPaymentsClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final RestClient restClient;
    private final TossPaymentsProperties properties;

    @Override
    public TossConfirmResult confirmPayment(String paymentKey, String orderId, Money amount) {
        TossConfirmApiResponse response = restClient.post()
                .uri(CONFIRM_URL)
                .header("Authorization", basicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "paymentKey", paymentKey,
                        "orderId", orderId,
                        "amount", amount.getValue()
                ))
                .exchange((request, response1) -> {
                    if (!response1.getStatusCode().is2xxSuccessful()) {
                        throw toApiException(response1.getStatusCode(), response1);
                    }
                    return response1.bodyTo(TossConfirmApiResponse.class);
                });

        return new TossConfirmResult(
                response.paymentKey(),
                response.orderId(),
                Money.of(response.totalAmount())
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