package site.auctionservice.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import site.auctionservice.application.port.dto.WalletHoldInfo;
import site.auctionservice.domain.Money;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletHttpClientTest {

    private MockRestServiceServer server;
    private WalletHttpClient walletHttpClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        walletHttpClient = new WalletHttpClient(builder.build());
    }

    @Test
    @DisplayName("지갑 서버 응답이 성공이면 홀드 정보를 반환한다")
    void testHold_success_returnsWalletHoldInfo() {
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {"holdId": 1, "releasedHoldId": null, "balanceAfter": 87000},
                          "error": {"code": null, "message": null}
                        }
                        """, MediaType.APPLICATION_JSON));

        WalletHoldInfo result = walletHttpClient.hold(1L, 2L, Money.of(13_000L));

        assertThat(result).isEqualTo(new WalletHoldInfo(1L, null, BigDecimal.valueOf(87_000)));
        server.verify();
    }

    @Test
    @DisplayName("지갑 서버가 200인데 success:false/데이터 없음이면 계약 위반 예외를 던진다 (pointwallet은 실제로는 이 형태로 응답하지 않음 — 방어 코드)")
    void testHold_malformedSuccessBody_throwsUpstreamContractViolation() {
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("""
                        {
                          "success": false,
                          "data": null,
                          "error": {"code": "WALLET-001", "message": "잔액이 부족합니다."}
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> walletHttpClient.hold(1L, 2L, Money.of(13_000L)))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.UPSTREAM_CONTRACT_VIOLATION);
        server.verify();
    }

    @Test
    @DisplayName("지갑 서버가 400(Bad Request)을 반환하면 INSUFFICIENT_BALANCE 예외를 던진다")
    void testHold_badRequest_throwsAuctionException() {
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> walletHttpClient.hold(1L, 2L, Money.of(13_000L)))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.INSUFFICIENT_BALANCE);
        server.verify();
    }

    @Test
    @DisplayName("지갑 서버가 422(잔액 부족)를 반환하면 INSUFFICIENT_BALANCE 예외를 던진다")
    void testHold_insufficientBalance_throwsAuctionException() {
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT));

        assertThatThrownBy(() -> walletHttpClient.hold(1L, 2L, Money.of(13_000L)))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.INSUFFICIENT_BALANCE);
        server.verify();
    }
}
