package site.auctionservice.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import site.common.response.ApiResponse;
import site.auctionservice.application.port.MemberPort;
import site.auctionservice.exception.UpstreamContractViolationException;

@Component
public class MemberHttpClient implements MemberPort {

    private final RestClient auctionMemberRestClient;

    public MemberHttpClient(@Qualifier("auctionMemberRestClient") RestClient auctionMemberRestClient) {
        this.auctionMemberRestClient = auctionMemberRestClient;
    }

    @Override
    public String getNickname(Long memberId) {
        ApiResponse<NicknameResponse> body;
        try {
            body = auctionMemberRestClient.get()
                    .uri("/internal/v1/members/{memberId}/profile", memberId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (HttpClientErrorException e) {
            throw new UpstreamContractViolationException(
                    "회원 프로필 조회 실패 — memberId: " + memberId + ", 상태: " + e.getStatusCode());
        }

        if (body == null || body.getData() == null) {
            throw new UpstreamContractViolationException("회원 프로필 응답 본문이 비어 있습니다 — memberId: " + memberId);
        }
        return body.getData().nickname();
    }

    private record NicknameResponse(String nickname) {}

}
