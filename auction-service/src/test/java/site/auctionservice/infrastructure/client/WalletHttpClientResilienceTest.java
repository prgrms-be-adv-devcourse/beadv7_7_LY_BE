package site.auctionservice.infrastructure.client;

import io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import site.auctionservice.domain.Money;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * WalletHttpClient.hold()에 실제로 붙는 resilience4j 데코레이터(@Retry/@CircuitBreaker/@Bulkhead)가
 * application.yml의 walletHold 설정 의도대로 동작하는지 검증한다.
 *
 * WalletHttpClientTest는 WalletHttpClient를 new로 직접 생성해 AOP 프록시 없이 hold() 내부의 예외 번역
 * 로직만 본다 — 이 클래스는 반대로 Spring AOP 프록시(CGLIB)를 실제로 태워 데코레이터가 걸린 상태를 검증한다.
 */
class WalletHttpClientResilienceTest {

    @Configuration(proxyBeanMethods = false)
    @EnableResilientMethods
    static class ResilientMethodsTestConfig {
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        CircuitBreakerAutoConfiguration.class,
                        RetryAutoConfiguration.class,
                        BulkheadAutoConfiguration.class))
                .withUserConfiguration(ResilientMethodsTestConfig.class)
                .withPropertyValues("spring.aop.proxy-target-class=true");
    }

    private static String successBody() {
        return """
                {
                  "success": true,
                  "data": {"holdId": 1, "releasedHoldId": null, "balanceAfter": 87000},
                  "error": {"code": null, "message": null}
                }
                """;
    }

    private static String malformedBody() {
        return """
                {
                  "success": false,
                  "data": null,
                  "error": {"code": "WALLET-500", "message": "일시적 오류"}
                }
                """;
    }

    @Test
    @DisplayName("[Retry] 연결/응답 타임아웃(ResourceAccessException)이 나면 재시도해서 두 번째 시도에서 성공한다")
    void retry_retriesOnResourceAccessException_andSucceedsOnSecondAttempt() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        // 1차 시도: 소켓 타임아웃(IOException) → RestClient가 ResourceAccessException으로 감싸서 던짐
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(request -> {
                    throw new SocketTimeoutException("simulated read/connect timeout");
                });
        // 2차 시도(재시도): 정상 응답
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        baseRunner()
                .withPropertyValues(
                        "resilience4j.retry.instances.walletHold.max-attempts=2",
                        "resilience4j.retry.instances.walletHold.wait-duration=10ms",
                        "resilience4j.retry.instances.walletHold.retry-exceptions[0]=org.springframework.web.client.ResourceAccessException",
                        "resilience4j.circuitbreaker.instances.walletHold.minimum-number-of-calls=1000",
                        "resilience4j.bulkhead.instances.walletHold.max-concurrent-calls=10")
                .withBean("walletHttpClient", WalletHttpClient.class, () -> new WalletHttpClient(restClient))
                .run(context -> {
                    WalletHttpClient walletHttpClient = context.getBean(WalletHttpClient.class);

                    assertThat(walletHttpClient.hold(1L, 2L, Money.of(13_000L)).holdId()).isEqualTo(1L);
                    server.verify();
                });
    }

    @Test
    @DisplayName("[Retry] 비즈니스 예외(잔액 부족)는 재시도하지 않고, fallback으로도 감싸지 않은 채 그대로 전파한다")
    void retry_doesNotRetryBusinessException_andFallbackDoesNotMaskIt() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        // 요청을 정확히 한 번만 기대한다 — 재시도가 실제로 일어나면 MockRestServiceServer가
        // "예상 밖 요청"으로 실패시켜 이 테스트가 재시도 여부까지 함께 검증한다.
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT));

        baseRunner()
                .withPropertyValues(
                        "resilience4j.retry.instances.walletHold.max-attempts=2",
                        "resilience4j.retry.instances.walletHold.wait-duration=10ms",
                        "resilience4j.retry.instances.walletHold.retry-exceptions[0]=org.springframework.web.client.ResourceAccessException",
                        "resilience4j.retry.instances.walletHold.ignore-exceptions[0]=site.auctionservice.exception.WalletBusinessException",
                        "resilience4j.circuitbreaker.instances.walletHold.minimum-number-of-calls=1000",
                        "resilience4j.circuitbreaker.instances.walletHold.ignore-exceptions[0]=site.auctionservice.exception.WalletBusinessException",
                        "resilience4j.bulkhead.instances.walletHold.max-concurrent-calls=10")
                .withBean("walletHttpClient", WalletHttpClient.class, () -> new WalletHttpClient(restClient))
                .run(context -> {
                    WalletHttpClient walletHttpClient = context.getBean(WalletHttpClient.class);

                    assertThatThrownBy(() -> walletHttpClient.hold(1L, 2L, Money.of(13_000L)))
                            .isInstanceOf(AuctionException.class)
                            .extracting(e -> ((AuctionException) e).getErrorCode())
                            .as("fallback()이 이미 번역된 AuctionException(WalletBusinessException 포함)은 재포장하지 않고 그대로 재던져야 원래 코드(INSUFFICIENT_BALANCE)가 살아남는다")
                            .isEqualTo(AuctionErrorCode.INSUFFICIENT_BALANCE);
                    server.verify();
                });
    }

    @Test
    @DisplayName("[CircuitBreaker] 지갑 응답이 비정상(malformed)이면 실패로 집계하고, 임계치를 넘기면 서킷이 열려 이후 요청은 fallback으로 WALLET_SERVICE_UNAVAILABLE을 던진다")
    void circuitBreaker_recordsMalformedResponseAsFailure_opensAndFallsBackToServiceUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        // 서킷이 열리기 전 마지막 실제 호출 1건만 서버에 기대한다 — 그 이후 요청은 서킷이 막아 실제 호출이 없어야 한다.
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(malformedBody(), MediaType.APPLICATION_JSON));

        baseRunner()
                .withPropertyValues(
                        "resilience4j.retry.instances.walletHold.max-attempts=1",
                        "resilience4j.circuitbreaker.instances.walletHold.sliding-window-type=COUNT_BASED",
                        "resilience4j.circuitbreaker.instances.walletHold.sliding-window-size=1",
                        "resilience4j.circuitbreaker.instances.walletHold.minimum-number-of-calls=1",
                        "resilience4j.circuitbreaker.instances.walletHold.failure-rate-threshold=1",
                        "resilience4j.circuitbreaker.instances.walletHold.wait-duration-in-open-state=60s",
                        "resilience4j.circuitbreaker.instances.walletHold.automatic-transition-from-open-to-half-open-enabled=false",
                        "resilience4j.circuitbreaker.instances.walletHold.record-exceptions[0]=site.auctionservice.exception.AuctionException",
                        "resilience4j.circuitbreaker.instances.walletHold.ignore-exceptions[0]=site.auctionservice.exception.WalletBusinessException",
                        "resilience4j.bulkhead.instances.walletHold.max-concurrent-calls=10")
                .withBean("walletHttpClient", WalletHttpClient.class, () -> new WalletHttpClient(restClient))
                .run(context -> {
                    WalletHttpClient walletHttpClient = context.getBean(WalletHttpClient.class);

                    // 1번째 호출: 실제 hold() 시도 → malformed 응답 → 실패로 집계돼 서킷이 즉시 OPEN.
                    // 이 호출 자신의 예외는 이미 번역된 AuctionException이므로 fallback이 다시 감싸지 않고
                    // 원래 코드(UPSTREAM_CONTRACT_VIOLATION)를 그대로 던져야 한다.
                    assertThatThrownBy(() -> walletHttpClient.hold(1L, 2L, Money.of(13_000L)))
                            .isInstanceOf(AuctionException.class)
                            .extracting(e -> ((AuctionException) e).getErrorCode())
                            .isEqualTo(AuctionErrorCode.UPSTREAM_CONTRACT_VIOLATION);

                    // 2번째 호출: 서킷이 이미 OPEN이라 실제 호출 없이 즉시 fallback.
                    assertThatThrownBy(() -> walletHttpClient.hold(1L, 2L, Money.of(13_000L)))
                            .isInstanceOf(AuctionException.class)
                            .extracting(e -> ((AuctionException) e).getErrorCode())
                            .as("서킷 OPEN 상태의 CallNotPermittedException도 fallback을 거쳐 WALLET_SERVICE_UNAVAILABLE이어야 한다")
                            .isEqualTo(AuctionErrorCode.WALLET_SERVICE_UNAVAILABLE);

                    server.verify();
                });
    }

    @Test
    @DisplayName("[Bulkhead] 동시 허용량이 가득 차면 즉시 거절되고 fallback으로 WALLET_SERVICE_UNAVAILABLE을 던진다")
    void bulkhead_rejectsWhenFull_andFallsBackToServiceUnavailable() throws InterruptedException {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        java.util.concurrent.CountDownLatch releaseFirstCall = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch firstCallStarted = new java.util.concurrent.CountDownLatch(1);

        // 첫 번째 호출은 releaseFirstCall이 열릴 때까지 응답을 붙들어 Bulkhead 슬롯을 점유한 채로 둔다.
        server.expect(requestTo("http://localhost:8080/internal/v1/wallet/hold"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(request -> {
                    firstCallStarted.countDown();
                    try {
                        releaseFirstCall.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException(e);
                    }
                    return withSuccess(successBody(), MediaType.APPLICATION_JSON).createResponse(request);
                });

        baseRunner()
                .withPropertyValues(
                        "resilience4j.retry.instances.walletHold.max-attempts=1",
                        "resilience4j.circuitbreaker.instances.walletHold.minimum-number-of-calls=1000",
                        "resilience4j.bulkhead.instances.walletHold.max-concurrent-calls=1",
                        "resilience4j.bulkhead.instances.walletHold.max-wait-duration=0")
                .withBean("walletHttpClient", WalletHttpClient.class, () -> new WalletHttpClient(restClient))
                .run(context -> {
                    WalletHttpClient walletHttpClient = context.getBean(WalletHttpClient.class);

                    Thread firstCaller = new Thread(() -> walletHttpClient.hold(1L, 2L, Money.of(13_000L)));
                    firstCaller.start();
                    assertThat(firstCallStarted.await(2, java.util.concurrent.TimeUnit.SECONDS))
                            .as("첫 번째 호출이 Bulkhead 슬롯을 점유할 때까지 대기")
                            .isTrue();

                    // 슬롯이 가득 찬 상태에서 두 번째 호출: 즉시(대기 없이) 거절되어야 한다.
                    assertThatThrownBy(() -> walletHttpClient.hold(3L, 4L, Money.of(13_000L)))
                            .isInstanceOf(AuctionException.class)
                            .extracting(e -> ((AuctionException) e).getErrorCode())
                            .isEqualTo(AuctionErrorCode.WALLET_SERVICE_UNAVAILABLE);

                    releaseFirstCall.countDown();
                    firstCaller.join(2000);
                    server.verify();
                });
    }
}
