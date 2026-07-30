package site.coreservice.auction.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import site.common.response.ApiResponse;
import site.coreservice.auction.application.port.WalletPort;
import site.coreservice.auction.application.port.dto.WalletHoldInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.util.Map;

@Component
public class WalletHttpClient implements WalletPort {

    private final RestClient auctionWalletRestClient;

    public WalletHttpClient(@Qualifier("auctionWalletRestClient") RestClient auctionWalletRestClient) {
        this.auctionWalletRestClient = auctionWalletRestClient;
    }

    @Override
    public WalletHoldInfo hold(Long auctionId, Long memberId, Money amount) {
        ApiResponse<WalletHoldInfo> body;
        try {
            body = auctionWalletRestClient.put()
                    .uri("/internal/v1/wallet/hold")
                    .body(Map.of(
                            "auctionId", auctionId,
                            "memberId", memberId,
                            "amount", amount.getValue()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (HttpClientErrorException.NotFound e) {
            throw new AuctionException(AuctionErrorCode.WALLET_NOT_FOUND);
        } catch (HttpClientErrorException.UnprocessableContent e) {
            throw new AuctionException(AuctionErrorCode.INSUFFICIENT_BALANCE);
        }

        if (body == null || !body.isSuccess() || body.getData() == null) {
            throw new AuctionException(AuctionErrorCode.WALLET_HOLD_FAILED);
        }
        return body.getData();
    }
}
