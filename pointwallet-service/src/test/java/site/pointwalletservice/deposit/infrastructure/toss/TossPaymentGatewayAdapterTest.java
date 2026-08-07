package site.pointwalletservice.deposit.infrastructure.toss;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import site.pointwalletservice.deposit.domain.PgApproveResult;
import site.pointwalletservice.deposit.domain.PgCancelResult;
import site.pointwalletservice.shared.Money;


@DisplayName("TossPaymentGatewayAdapter")
class TossPaymentGatewayAdapterTest {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String PROVIDER_TX_ID = "toss-payment-key-1";
    private static final String ORDER_ID = "DEPOSIT-ORDER-1";
    private static final Money AMOUNT = Money.of(10_000);

    private MockRestServiceServer mockServer;
    private TossPaymentGatewayAdapter sut;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        TossPaymentsProperties properties = new TossPaymentsProperties("test_ck_dummy", "test_sk_dummy"); // 실제 필드명에 맞게 조정
        sut = new TossPaymentGatewayAdapter(restClient, properties);
    }

    @Test
    @DisplayName("approve 성공 시 Idempotency-Key를 orderId 기준으로 보내고 결과를 매핑한다")
    void approve_성공하면_결과를_매핑한다() {
        mockServer.expect(requestTo(CONFIRM_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "DEPOSIT-CONFIRM-" + ORDER_ID))
                .andRespond(withSuccess("""
                        {"paymentKey":"%s","orderId":"%s","totalAmount":10000}
                        """.formatted(PROVIDER_TX_ID, ORDER_ID), MediaType.APPLICATION_JSON));

        PgApproveResult result = sut.approve(PROVIDER_TX_ID, ORDER_ID, AMOUNT);

        assertThat(result.providerTxId()).isEqualTo(PROVIDER_TX_ID);
        assertThat(result.orderId()).isEqualTo(ORDER_ID);
        assertThat(result.approvedAmount()).isEqualTo(AMOUNT);
        mockServer.verify();
    }

    @Test
    @DisplayName("연결 실패(ResourceAccessException)면 최대 2회까지 재시도하다가 결국 실패한다")
    void approve_연결실패면_재시도하다가_실패한다() {
        mockServer.expect(requestTo(CONFIRM_URL)).andRespond(request -> { throw new IOException("connect timeout"); });
        mockServer.expect(requestTo(CONFIRM_URL)).andRespond(request -> { throw new IOException("connect timeout"); });

        assertThatThrownBy(() -> sut.approve(PROVIDER_TX_ID, ORDER_ID, AMOUNT))
                .isInstanceOf(ResourceAccessException.class);

        mockServer.verify(); // 등록한 두 expect가 실제로 다 소진됐는지 = 정확히 2번 재시도했는지 검증
    }

    @Test
    @DisplayName("4xx 비즈니스 오류면 재시도 없이 바로 예외가 전파된다")
    void approve_4xx면_재시도없이_바로_예외() {
        mockServer.expect(requestTo(CONFIRM_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"code\":\"ALREADY_PROCESSED_PAYMENT\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> sut.approve(PROVIDER_TX_ID, ORDER_ID, AMOUNT))
                .isInstanceOf(TossPaymentsApiException.class);

        mockServer.verify(); // expect가 하나뿐이므로, 이게 통과하면 재시도가 없었다는 뜻
    }

    @Test
    @DisplayName("cancel 성공 시 Idempotency-Key를 providerTxId 기준으로 보내고 결과를 매핑한다")
    void cancel_성공하면_결과를_매핑한다() {
        String cancelUrl = "https://api.tosspayments.com/v1/payments/" + PROVIDER_TX_ID + "/cancel";
        mockServer.expect(requestTo(cancelUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "DEPOSIT-CANCEL-" + PROVIDER_TX_ID))
                .andRespond(withSuccess("""
                        {"paymentKey":"%s","cancels":[{"transactionKey":"cancel-tx-1","cancelAmount":10000}]}
                        """.formatted(PROVIDER_TX_ID), MediaType.APPLICATION_JSON));

        PgCancelResult result = sut.cancel(PROVIDER_TX_ID, "단순 변심", AMOUNT);

        assertThat(result.providerTxId()).isEqualTo(PROVIDER_TX_ID);
        assertThat(result.providerCancelTxId()).isEqualTo("cancel-tx-1");
        assertThat(result.canceledAmount()).isEqualTo(AMOUNT);
        mockServer.verify();
    }
}