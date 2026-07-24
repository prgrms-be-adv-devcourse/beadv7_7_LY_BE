package site.coreservice.auction.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import site.coreservice.auction.application.port.MemberPort;

@Component
@Profile("local")
@RequiredArgsConstructor
public class MockMemberClient implements MemberPort {

    @Override
    public String getNickname(Long memberId) {
        return "vinyl_king";
    }
}
