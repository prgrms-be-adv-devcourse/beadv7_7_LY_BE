package site.pointwalletservice.deposit.infrastructure.toss;
import org.springframework.http.HttpStatusCode;
import site.common.exception.BusinessException;
import site.pointwalletservice.deposit.exception.DepositErrorCode;

public class TossPaymentsApiException extends BusinessException {

    private final HttpStatusCode statusCode;

    public TossPaymentsApiException(HttpStatusCode statusCode, String responseBody) {
        super(DepositErrorCode.PG_API_ERROR,
                "토스페이먼츠 API 호출 실패 (status=" + statusCode + "): " + responseBody);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}