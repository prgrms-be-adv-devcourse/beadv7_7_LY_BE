package site.coreservice.pointwallet.deposit.infrastructure.toss;

import org.springframework.http.HttpStatusCode;

public class TossPaymentsApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public TossPaymentsApiException(HttpStatusCode statusCode, String responseBody) {
        super("토스페이먼츠 승인 API 호출 실패 (status=" + statusCode + "): " + responseBody);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}