package site.coreservice.auction.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import site.coreservice.auction.application.port.dto.WalletHoldInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
    @DisplayName("지갑 서버 응답이 실패면 예외를 던진다")
    void testHold_failureResponse_throwsAuctionException() {
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
                .isEqualTo(AuctionErrorCode.WALLET_HOLD_FAILED);
        server.verify();
    }
}
