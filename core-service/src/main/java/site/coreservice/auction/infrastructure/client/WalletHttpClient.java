package site.coreservice.auction.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.common.response.ApiResponse;
import site.coreservice.auction.application.port.WalletPort;
import site.coreservice.auction.application.port.dto.WalletHoldInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.util.Map;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class WalletHttpClient implements WalletPort {

    @Qualifier("auctionRestClient")
    private final RestClient auctionRestClient;

    @Override
    public WalletHoldInfo hold(Long auctionId, Long memberId, Money amount) {
        ApiResponse<WalletHoldInfo> body = auctionRestClient.put()
                .uri("/internal/v1/wallet/hold")
                .body(Map.of(
                        "auctionId", auctionId,
                        "memberId", memberId,
                        "amount", amount.getValue()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (body == null || !body.isSuccess()) {
            throw new AuctionException(AuctionErrorCode.WALLET_HOLD_FAILED);
        }
        return body.getData();
    }
}
