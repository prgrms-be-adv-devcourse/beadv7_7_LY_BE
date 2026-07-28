package site.coreservice.product.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import site.coreservice.product.domain.ClosedAuction;
import site.coreservice.product.domain.MediaCondition;
import site.coreservice.product.exception.AuctionContractViolationException;
import tools.jackson.databind.json.JsonMapper;

class AuctionSnapshotHttpClientTest {

    private static final String BASE_URL = "http://auction.test";
    private static final String URI = BASE_URL + "/internal/v1/auctions/7";

    private MockRestServiceServer server;
    private AuctionSnapshotHttpClient client;

    @BeforeEach
    void setUp() {
        // 운영 빈과 같은 조립 경로를 쓴다 — 컨버터 설정이 갈리면 테스트가 실물을 대변하지 못한다
        RestClient.Builder builder = ProductAuctionClientConfig.configure(
                RestClient.builder(), BASE_URL, JsonMapper.builder().build());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AuctionSnapshotHttpClient(builder.build());
    }

    private String successBody(String status, String itemCondition, String finalPrice) {
        return """
                {"success":true,"data":{"auctionId":7,"productId":3,"itemCondition":"%s","bidCount":3,
                 "finalPrice":%s,"endAt":"2026-07-27T20:31:00","status":"%s"},"error":null}
                """.formatted(itemCondition, finalPrice, status);
    }

    @Test
    @DisplayName("낙찰 응답을 ClosedAuction으로 옮겨 돌려준다")
    void findClosedAuction_낙찰_매핑() {
        // given
        server.expect(requestTo(URI))
                .andRespond(withSuccess(successBody("ENDED_WON", "NEAR_MINT", "15000"), MediaType.APPLICATION_JSON));

        // when
        Optional<ClosedAuction> found = client.findClosedAuction(7L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().mediaCondition()).isEqualTo(MediaCondition.NEAR_MINT);
        assertThat(found.get().finalPrice()).isEqualTo(15_000L);
        assertThat(found.get().bidCount()).isEqualTo(3);
        assertThat(found.get().isClosed()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("진행중 경매도 그대로 돌려주되 마감으로 보지 않는다")
    void findClosedAuction_진행중() {
        // given
        server.expect(requestTo(URI))
                .andRespond(withSuccess(successBody("RUNNING", "MINT", "15000"), MediaType.APPLICATION_JSON));

        // when
        Optional<ClosedAuction> found = client.findClosedAuction(7L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().isClosed()).isFalse();
    }

    @Test
    @DisplayName("모르는 상태값은 예외 없이 마감이 아닌 것으로 취급한다")
    void findClosedAuction_모르는_상태값() {
        // given: 경매가 낙찰 상태명을 바꾸면 예외 없이 시세가 0건이 되는 구조라, 그 동작을 고정해 둔다
        server.expect(requestTo(URI))
                .andRespond(withSuccess(successBody("ENDED_XXX", "MINT", "15000"), MediaType.APPLICATION_JSON));

        // when
        Optional<ClosedAuction> found = client.findClosedAuction(7L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().isClosed()).isFalse();
    }

    @Test
    @DisplayName("경매가 없다고 답한 404는 빈 결과로 바꾼다")
    void findClosedAuction_경매없음_404() {
        // given
        server.expect(requestTo(URI))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":false,"data":null,
                                 "error":{"code":"AERR-5002","message":"경매를 찾을 수 없습니다"}}
                                """));

        // when
        Optional<ClosedAuction> found = client.findClosedAuction(7L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("경로가 틀려 생긴 404는 빈 결과로 바꾸지 않고 계약 위반으로 올린다")
    void findClosedAuction_경로오류_404() {
        // given: 경로 오타나 경매 쪽 경로 변경도 404다. 이 프로젝트에서 없는 경로는 공통 핸들러가
        // GERR-0002로 감싸 돌려주므로 봉투는 정상 파싱되고 코드만 다르다 — 실전에서 실제로 오는 형태
        server.expect(requestTo(URI))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"success":false,"data":null,
                                 "error":{"code":"GERR-0002","message":"존재하지 않는 리소스입니다."}}
                                """));

        // when-then: "경매 없음"으로 삼키면 우리 버그가 보낸 쪽 탓으로 기록된다
        assertThatThrownBy(() -> client.findClosedAuction(7L))
                .isInstanceOf(AuctionContractViolationException.class);
    }

    @Test
    @DisplayName("봉투 형태가 아닌 404도 계약 위반으로 올린다")
    void findClosedAuction_해석불가_404() {
        // given: 공통 핸들러를 타지 않는 404(프록시·게이트웨이 등)는 본문 파싱 자체가 안 된다.
        // 위 케이스와 갈래가 달라 따로 덮는다 — 이쪽은 "파싱 실패", 위는 "파싱 성공인데 코드 불일치"
        server.expect(requestTo(URI))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html><body>404 Not Found</body></html>"));

        // when-then
        assertThatThrownBy(() -> client.findClosedAuction(7L))
                .isInstanceOf(AuctionContractViolationException.class);
    }

    @Test
    @DisplayName("서버 오류는 감싸지 않고 그대로 올려 재시도 대상으로 남긴다")
    void findClosedAuction_5xx_전파() {
        // given
        server.expect(requestTo(URI)).andRespond(withServerError());

        // when-then: 계약위반으로 감싸면 재시도하면 될 일시 장애가 "재시도 무의미"로 분류된다
        assertThatThrownBy(() -> client.findClosedAuction(7L))
                .isInstanceOf(RestClientException.class)
                .isNotInstanceOf(AuctionContractViolationException.class);
    }

    @Test
    @DisplayName("성공 응답인데 알맹이가 없으면 계약 위반으로 올린다")
    void findClosedAuction_data_null() {
        // given
        server.expect(requestTo(URI))
                .andRespond(withSuccess("{\"success\":true,\"data\":null,\"error\":null}",
                        MediaType.APPLICATION_JSON));

        // when-then
        assertThatThrownBy(() -> client.findClosedAuction(7L))
                .isInstanceOf(AuctionContractViolationException.class);
    }

    @Test
    @DisplayName("소수부 있는 낙찰가는 계약 위반으로 올린다")
    void findClosedAuction_소수부_낙찰가() {
        // given
        server.expect(requestTo(URI))
                .andRespond(withSuccess(successBody("ENDED_WON", "MINT", "15000.75"), MediaType.APPLICATION_JSON));

        // when-then
        assertThatThrownBy(() -> client.findClosedAuction(7L))
                .isInstanceOf(AuctionContractViolationException.class);
    }

    @Test
    @DisplayName("인증 실패 같은 4xx는 계약 위반으로 올린다")
    void findClosedAuction_401_계약위반() {
        // given: 내부 API에 접근 통제가 붙으면 자격증명 없는 호출이 여기로 온다
        server.expect(requestTo(URI)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // when-then: 다시 넣어도 같은 결과라 "재시도하면 됨"으로 분류되면 안 된다
        assertThatThrownBy(() -> client.findClosedAuction(7L))
                .isInstanceOf(AuctionContractViolationException.class);
    }

    @Test
    @DisplayName("호출량 제한(429)은 감싸지 않고 그대로 올려 재시도 대상으로 남긴다")
    void findClosedAuction_429_전파() {
        // given
        server.expect(requestTo(URI)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        // when-then: 잠시 뒤면 풀리는 실패라 계약 위반과 갈래가 다르다
        assertThatThrownBy(() -> client.findClosedAuction(7L))
                .isInstanceOf(RestClientException.class)
                .isNotInstanceOf(AuctionContractViolationException.class);
    }
}
