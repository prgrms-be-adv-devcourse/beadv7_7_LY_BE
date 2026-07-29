package site.coreservice.auction.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.common.response.ApiResponse;
import site.coreservice.auction.application.port.MemberPort;

/**
 * member 도메인 API가 아직 개발/검증 완료되지 않아 API 명세서 기준으로 우선 작성함. 명세 변경 시 수정될 수 있음.
 * 로컬에서는 MockMemberClient로 대체하고, 추후 실제 API 확정되면 이 클라이언트로 교체 예정.
 */
@Component
@RequiredArgsConstructor
public class MemberHttpClient implements MemberPort {

    @Qualifier("auctionRestClient")
    private final RestClient auctionRestClient;

    @Override
    public String getNickname(Long memberId) {
        ApiResponse<NicknameResponse> body = auctionRestClient.get()
                .uri("/internal/v1/members/{memberId}/profile", memberId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return body.getData().nickname();
    }

    private record NicknameResponse(String nickname) {}

}
