package site.coreservice.pointwallet.withdraw.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import site.coreservice.pointwallet.withdraw.application.port.BankAccount;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MemberBankAccountHttpClientTest {

    private MockRestServiceServer server;
    private MemberBankAccountHttpClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8081");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MemberBankAccountHttpClient(builder.build());
    }

    @Test
    @DisplayName("member 내부 API(/internal/v1/members/{id}/bank-account)를 호출해 계좌 정보를 반환한다")
    void testGetBankAccount_success_returnsBankAccount() {
        server.expect(requestTo("http://localhost:8081/internal/v1/members/10/bank-account"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "bankName": "하나은행",
                            "accountNumber": "123-123456-12301",
                            "depositorName": "홍길동"
                          },
                          "error": {"code": null, "message": null}
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<BankAccount> result = client.getBankAccount(10L);

        assertThat(result).contains(new BankAccount("하나은행", "123-123456-12301", "홍길동"));
        server.verify();
    }

    @Test
    @DisplayName("계좌 미등록(404)이면 빈 Optional을 반환한다")
    void testGetBankAccount_notFound_returnsEmpty() {
        server.expect(requestTo("http://localhost:8081/internal/v1/members/10/bank-account"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success": false, "data": null, "error": {"code": "MERR-0003", "message": "등록된 계좌가 없습니다."}}
                                """));

        Optional<BankAccount> result = client.getBankAccount(10L);

        assertThat(result).isEmpty();
        server.verify();
    }
}
