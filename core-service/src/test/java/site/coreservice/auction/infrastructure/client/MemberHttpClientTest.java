package site.coreservice.auction.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import site.coreservice.auction.exception.UpstreamContractViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;

class MemberHttpClientTest {

    private MockRestServiceServer server;
    private MemberHttpClient memberHttpClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        memberHttpClient = new MemberHttpClient(builder.build());
    }

    @Test
    @DisplayName("member 서버 응답을 파싱해 닉네임을 반환한다")
    void testGetNickname_success_returnsNickname() {
        server.expect(requestTo("http://localhost:8080/internal/v1/members/10/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"nickname":"testUser"},"error":{"code":null,"message":null}}
                        """, MediaType.APPLICATION_JSON));

        String nickname = memberHttpClient.getNickname(10L);

        assertThat(nickname).isEqualTo("testUser");
        server.verify();
    }

    @Test
    @DisplayName("member 서버가 404를 반환하면 계약 위반 예외를 던진다")
    void testGetNickname_notFound_throwsContractViolation() {
        server.expect(requestTo("http://localhost:8080/internal/v1/members/10/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> memberHttpClient.getNickname(10L))
                .isInstanceOf(UpstreamContractViolationException.class);
        server.verify();
    }

    @Test
    @DisplayName("member 서버 응답이 성공인데 data가 없으면 계약 위반 예외를 던진다")
    void testGetNickname_emptyData_throwsContractViolation() {
        server.expect(requestTo("http://localhost:8080/internal/v1/members/10/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"success":true,"data":null,"error":{"code":null,"message":null}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> memberHttpClient.getNickname(10L))
                .isInstanceOf(UpstreamContractViolationException.class);
        server.verify();
    }
}
