package site.auctionservice.infrastructure.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import site.auctionservice.exception.WalletBusinessException;
import site.common.response.ApiResponse;
import site.auctionservice.application.port.WalletPort;
import site.auctionservice.application.port.dto.WalletHoldInfo;
import site.auctionservice.domain.Money;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.util.Map;

@Slf4j
@Component
public class WalletHttpClient implements WalletPort {

    private final RestClient auctionWalletRestClient;

    public WalletHttpClient(@Qualifier("auctionWalletRestClient") RestClient auctionWalletRestClient) {
        this.auctionWalletRestClient = auctionWalletRestClient;
    }

    // 데코레이터 중첩 : Retry(Circuit Breaker(Bulkhead(httpclient timeout)))
    // fallbackMethod는 반드시 가장 바깥(Retry)에 붙여야 한다 — CircuitBreaker처럼 안쪽 데코레이터에 붙이면
    // resilience4j가 시도 1회마다(재시도 전에!) fallback으로 빠져버려 Retry가 원본 예외를 볼 기회조차 없어진다
    @Override
    @Retry(name = "walletHold", fallbackMethod = "fallback")
    @CircuitBreaker(name = "walletHold")
    @Bulkhead(name = "walletHold")
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
        }  catch (HttpClientErrorException.UnprocessableContent | HttpClientErrorException.BadRequest e) {
            throw new WalletBusinessException(AuctionErrorCode.INSUFFICIENT_BALANCE);
        } catch (HttpClientErrorException.NotFound e) {
            throw new WalletBusinessException(AuctionErrorCode.WALLET_NOT_FOUND);
        }

        // pointwallet-service의 HoldController/GlobalExceptionHandler는 비즈니스 실패를 항상 매칭되는
        // 4xx 상태코드(404/422 등)로 내려준다 — success:false+200이나 데이터 누락은 정상 흐름에서 나올 수
        // 없는 계약 위반이다(위 catch 블록이 실제 실패 경로를 이미 전담).
        if (body == null || !body.isSuccess() || body.getData() == null) {
            throw new AuctionException(AuctionErrorCode.UPSTREAM_CONTRACT_VIOLATION);
        }
        return body.getData();
    }

    // resilience4j의 fallbackMethod는 record/ignore-exceptions 설정과 무관하게 이 시그니처와 매치되는
    // 예외는 전부 여기로 넘긴다 — 이미 도메인 에러코드로 번역된 AuctionException(잔액부족/지갑없음/응답이상)은
    // 여기서 한 번 더 감싸지 않고 그대로 재던져야 각자의 원래 코드가 클라이언트까지 살아서 전파된다.
    private WalletHoldInfo fallback(Long auctionId, Long memberId, Money amount, Exception e) {
        if (e instanceof AuctionException auctionException) {
            throw auctionException;
        }
        log.warn("[WALLET_HOLD_FALLBACK] auctionId = {}, memberId={}, cause={}", auctionId, memberId, e.toString());
        throw new AuctionException(AuctionErrorCode.WALLET_SERVICE_UNAVAILABLE);
    }
}
