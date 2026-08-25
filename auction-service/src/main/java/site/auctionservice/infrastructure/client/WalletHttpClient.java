package site.auctionservice.infrastructure.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import site.auctionservice.exception.WalletBusinessException;
import site.auctionservice.exception.WalletHoldOutcomeUnknownException;
import site.common.response.ApiResponse;
import site.auctionservice.application.port.WalletPort;
import site.auctionservice.application.port.dto.WalletHoldInfo;
import site.auctionservice.domain.Money;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.net.ConnectException;
import java.net.UnknownHostException;
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
        } catch (ResourceAccessException e) {
            if (isConnectionPhaseFailure(e)) {
                throw e; // 연결 자체가 안 됨 - 요청 바이트가 전혀 전송되지 않았음이 보장되므로 재시도해도 안전(resilience4j retryExceptions 대상)
            }
            // 연결은 맺어졌는데 그 이후(요청 전송/응답 대기) 실패 - 서버가 이미 처리했을 수 있어 재시도가 위험하다
            // resilience4j retry의 ignoreExceptions에 걸려 있어 재시도되지 않고 곧장 fallback으로 간다.
            throw new WalletHoldOutcomeUnknownException(e);
        }

        // pointwallet-service의 HoldController/GlobalExceptionHandler는 비즈니스 실패를 항상 매칭되는
        // 4xx 상태코드(404/422 등)로 내려준다 — success:false+200이나 데이터 누락은 정상 흐름에서 나올 수
        // 없는 계약 위반이다(위 catch 블록이 실제 실패 경로를 이미 전담).
        if (body == null || !body.isSuccess() || body.getData() == null) {
            throw new AuctionException(AuctionErrorCode.UPSTREAM_CONTRACT_VIOLATION);
        }
        return body.getData();
    }

    // 연결(TCP 핸드셰이크/DNS) 단계 실패만 재시도 안전하다고 판단한다 - 이 경우 요청 바이트가 전혀 전송되지 않았음이 보장된다.
    // 커넥션 풀이 이미 연결을 재사용하는 구조라 연결이 맺어진 뒤(응답 대기 중 타임아웃, 연결 리셋 등)의 실패는
    // 서버가 이미 처리를 시작/완료했을 가능성을 배제할 수 없으므로 보수적으로 안전하지 않은 쪽으로 취급한다.
    private boolean isConnectionPhaseFailure(ResourceAccessException e) {
        Throwable cause = e.getCause();
        return cause instanceof ConnectException || cause instanceof UnknownHostException;
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
