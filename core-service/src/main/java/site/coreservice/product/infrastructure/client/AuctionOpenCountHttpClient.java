package site.coreservice.product.infrastructure.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.coreservice.product.application.port.AuctionOpenCountPort;
import site.coreservice.product.exception.AuctionContractViolationException;
import site.coreservice.product.infrastructure.client.dto.AuctionApiEnvelope;
import site.coreservice.product.infrastructure.client.dto.AuctionCountsPayload;

/**
 * 진행 중 경매 수 조회 창구의 구현. AuctionSnapshotHttpClient와 달리 실패를 여기서 갈라 처리하지
 * 않는다 — 이 조회는 없어도 목록 화면이 성립해서, 살릴지 말지는 호출자(ProductService)가 정한다.
 */
@Component
public class AuctionOpenCountHttpClient implements AuctionOpenCountPort {

    private final RestClient auctionApiRestClient;

    public AuctionOpenCountHttpClient(@Qualifier("auctionApiRestClient") RestClient auctionApiRestClient) {
        this.auctionApiRestClient = auctionApiRestClient;
    }

    @Override
    public Map<Long, Long> findOpenAuctionCounts(List<Long> productIds) {
        // id 목록을 그대로 넘긴다 — 문자열로 이어붙이면 인코딩까지 우리가 책임져야 한다
        AuctionApiEnvelope<AuctionCountsPayload> envelope = auctionApiRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/v1/auctions/counts")
                        .queryParam("productIds", productIds)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (envelope == null || envelope.data() == null) {
            throw new AuctionContractViolationException("성공 응답인데 본문이 비어 있습니다 — productIds: " + productIds);
        }
        return envelope.data().counts().stream()
                .collect(Collectors.toMap(AuctionCountsPayload.Count::productId,
                        AuctionCountsPayload.Count::openAuctionCount));
    }
}
