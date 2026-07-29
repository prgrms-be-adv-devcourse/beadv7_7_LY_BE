package site.coreservice.auction.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
}
