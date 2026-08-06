package site.coreservice.product.infrastructure.client;

import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import site.coreservice.product.application.port.AuctionSnapshotPort;
import site.coreservice.product.domain.price.ClosedAuction;
import site.coreservice.product.exception.AuctionContractViolationException;
import site.coreservice.product.infrastructure.client.dto.ApiError;
import site.coreservice.product.infrastructure.client.dto.AuctionApiEnvelope;
import site.coreservice.product.infrastructure.client.dto.AuctionSummaryPayload;

/**
 * 경매 조회 창구의 실제 구현. 세미 기간의 스텁을 대체한다.
 * <p>
 * 실패를 세 종류로 갈라 내보내는 것이 이 클래스의 핵심 책임이다.
 * <ul>
 * <li>경매가 "없다"고 답한 404 → 빈 결과. 정상적인 답이다</li>
 * <li>그 밖의 404·인증·경로·형식 문제(401·403·405·415 등) → 계약 위반. 다시 물어봐도 같은 답이라
 * 재시도가 의미 없다. 다만 408·429는 시간이 지나면 풀리는 별개의 갈래라 그대로 전파한다</li>
 * <li>5xx·타임아웃·연결 실패 → 그대로 올린다. 이것만 재시도하면 되는 실패다</li>
 * </ul>
 * 셋을 뭉뚱그리면 로그가 원인을 반대로 가리킨다. 특히 경로 오타도 404라서, 상태코드만 보고
 * "경매 없음"으로 처리하면 우리 버그를 보낸 쪽 탓으로 기록하게 된다.
 */
@Component
public class AuctionSnapshotHttpClient implements AuctionSnapshotPort {

    /** 경매가 "그런 경매 없다"고 답할 때 쓰는 코드. 이 값일 때만 빈 결과로 바꾼다. */
    private static final String AUCTION_NOT_FOUND_CODE = "AERR-5002";

    /** 4xx 중 이 둘은 잠시 뒤 다시 시도하면 풀린다. 나머지 4xx와 갈래가 다르다. */
    private static final Set<Integer> RETRYABLE_CLIENT_ERRORS =
            Set.of(HttpStatus.REQUEST_TIMEOUT.value(), HttpStatus.TOO_MANY_REQUESTS.value());

    private final RestClient auctionApiRestClient;

    public AuctionSnapshotHttpClient(@Qualifier("auctionApiRestClient") RestClient auctionApiRestClient) {
        this.auctionApiRestClient = auctionApiRestClient;
    }

    @Override
    public Optional<ClosedAuction> findClosedAuction(Long auctionId) {
        AuctionApiEnvelope<AuctionSummaryPayload> envelope;
        try {
            envelope = auctionApiRestClient.get()
                    .uri("/internal/v1/auctions/{auctionId}", auctionId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (HttpClientErrorException.NotFound e) {
            if (isAuctionNotFound(e)) {
                return Optional.empty();
            }
            throw new AuctionContractViolationException(
                    "경매 없음(" + AUCTION_NOT_FOUND_CODE + ")이 아닌 404입니다 — auctionId: " + auctionId
                            + ", 본문: " + e.getResponseBodyAsString());
        } catch (HttpClientErrorException e) {
            if (RETRYABLE_CLIENT_ERRORS.contains(e.getStatusCode().value())) {
                throw e;
            }
            throw new AuctionContractViolationException(
                    "경매 API가 요청을 거부했습니다 — auctionId: " + auctionId
                            + ", 상태: " + e.getStatusCode() + ", 본문: " + e.getResponseBodyAsString());
        }

        if (envelope == null || envelope.data() == null) {
            throw new AuctionContractViolationException("성공 응답인데 본문이 비어 있습니다 — auctionId: " + auctionId);
        }
        return Optional.of(envelope.data().toClosedAuction());
    }

    /** 본문을 못 읽으면 경매가 답한 404라고 볼 근거가 없으므로 false다. */
    private boolean isAuctionNotFound(HttpClientErrorException.NotFound e) {
        try {
            AuctionApiEnvelope<AuctionSummaryPayload> body =
                    e.getResponseBodyAs(new ParameterizedTypeReference<>() {
                    });
            ApiError error = (body != null) ? body.error() : null;
            return error != null && AUCTION_NOT_FOUND_CODE.equals(error.code());
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
