package site.auctionservice.infrastructure.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.resilience.InvocationRejectedException;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Resilience 구현 시 라이브러리 선택을 위한 테스트.
 *
 * 검증 대상은 세 개의 역할(재시도/서킷브레이커/동시성 제한)이고, 각 역할마다 R4j 구현과 Spring 코어 구현이 하나씩 있다 (서킷브레이커는 R4j에만 있음)
 * 재시도 = R4j {@code @Retry} vs Spring {@code @Retryable},
 * 동시성 제한 = R4j {@code @Bulkhead} vs Spring {@code @ConcurrencyLimit}.
 * 역할이 같은 두 구현(예: Retry와 Retryable, Bulkhead와 ConcurrencyLimit)은 실제로 하나를 골라 쓰는 것이지 같이 쓸 일이 없으므로 검증 대상이 아니다.
 * 이 테스트가 실측하는 건 역할이 다른 두 어노테이션(예: 재시도 × 서킷브레이커, 재시도 × 동시성 제한, 서킷브레이커 × 동시성 제한)을 같은 메서드에 함께 붙였을 때,
 * 그 둘 중 하나를 Spring 코어 구현으로 바꿔도 중첩 순서가 똑같이 유지되는지다.
 *
 * 각 역할 쌍마다 다음 구조로 나눠 테스트한다:
 * - 대조군: 두 역할을 순수 R4j 어노테이션끼리 조합했을 때의 순서(R4j 공식 권장 순서와 일치하는지 확인)
 * - 실험군(정방향/역방향 선언): 한쪽을 Spring 코어 어노테이션으로 바꿔 혼용했을 때도 같은 순서가 나오는지, 그리고 그 순서가 어노테이션 나열 순서와 무관한지
 */
class ResilienceDecoratorOrderTest {

    static class ProbeException extends RuntimeException {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableResilientMethods
    static class ResilientMethodsTestConfig {
    }

    // ==================== 재시도 × 서킷브레이커 ====================
    // 판별 원리: CircuitBreaker를 minimum-number-of-calls=1, failure-rate-threshold=1로 설정해 첫 실패 즉시 OPEN되게 만든다.
    // - Retry가 바깥(CB가 안쪽)이면: 재시도마다 매번 CB를 다시 통과해야 하므로, 첫 시도 실패 후 OPEN된 CB가
    //   두 번째 시도부터는 target을 아예 호출하지 못하게 막는다 → target 호출 횟수 = 1, 최종 예외 = CallNotPermittedException.
    // - CB가 바깥(Retry가 안쪽)이면: 재시도 전체가 CB 입장에서 "호출 1건"이므로, 재시도 예산을 전부 소진할
    //   때까지 target이 매번 실제로 호출된다 → target 호출 횟수 = 설정한 최대 시도 횟수, 최종 예외 = 원래 예외.

    /**
     * 대조군: 순수 R4j {@code @Retry} + {@code @CircuitBreaker}.
     */
    public static class PureR4jRetryCircuitBreakerProbe {
        // CGLIB 프록시(proxy-target-class)는 Objenesis로 생성자를 건너뛰고 인스턴스화되므로,
        // 인스턴스 필드 초기화가 프록시 객체 자신에게는 적용되지 않는다(target에만 적용됨).
        // 프록시를 통해서든 target을 통해서든 항상 같은 카운터를 보게 하려고 static으로 둔다.
        static final AtomicInteger invocations = new AtomicInteger();

        @Retry(name = "probeRetryCb")
        @CircuitBreaker(name = "probeRetryCb")
        public void call() {
            invocations.incrementAndGet();
            throw new ProbeException();
        }
    }

    /**
     * *-aspect-order 프로퍼티로 기본 순서를 뒤집을 수 있는지 확인하는 probe. 어노테이션 자체는 대조군과
     * 동일(순수 R4j Retry+CircuitBreaker)하지만, {@code invocations}가 static이라 대조군 클래스를 그대로
     * 재사용하면 두 테스트의 호출 횟수가 하나의 카운터에 누적된다(JUnit은 static 필드를 테스트 메서드
     * 사이에 리셋하지 않음) — 그래서 별도 클래스로 분리해 카운터를 독립시킨다.
     */
    public static class ReversedAspectOrderProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @Retry(name = "probeReversedOrder")
        @CircuitBreaker(name = "probeReversedOrder")
        public void call() {
            invocations.incrementAndGet();
            throw new ProbeException();
        }
    }

    /**
     * 실험군 1 : @Retryable(바깥 선언) + R4j @CircuitBreaker(안쪽 선언)
     */
    public static class RetryableOutsideCircuitBreakerProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @Retryable(includes = ProbeException.class, maxRetries = 2, delay = 10)
        @CircuitBreaker(name = "probeCrossRetryableCbFwd")
        public void call() {
            invocations.incrementAndGet();
            throw new ProbeException();
        }
    }

    /**
     * 실험군 2 : R4j @CircuitBreaker(바깥 선언) + @Retryable(안쪽 선언)
     */
    public static class CircuitBreakerOutsideRetryableProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @CircuitBreaker(name = "probeCrossRetryableCbRev")
        @Retryable(includes = ProbeException.class, maxRetries = 2, delay = 10)
        public void call() {
            invocations.incrementAndGet();
            throw new ProbeException();
        }
    }

    // ==================== 재시도 × 동시성 제한 ====================
    // 판별 원리: Bulkhead의 max-concurrent-calls=0은 "허용량 0"
    // — 동시 호출 경쟁 없이 첫 호출부터 항상 즉시 거부한다
    // (Spring의 ConcurrencyLimit도 value=0이면 동일하게 항상 즉시 거부 — 로컬 gradle 캐시 소스 ConcurrencyThrottleSupport.NO_CONCURRENCY=0 "prevents all access"로 확인).
    // 따라서 target은 어느 중첩 순서든 절대 호출되지 않으므로 target 호출 횟수로는 순서를 가릴 수 없다.
    // - 동시성 게이트가 R4j Bulkhead일 때: Bulkhead의 EventPublisher가 발행하는 "거부(rejected)" 이벤트 횟수를 센다.
    //   재시도가 바깥이면 재시도마다 Bulkhead를 다시 두드리므로 거부 이벤트 = 총 시도 횟수. 동시성 게이트가
    //   바깥이면 재시도 어노테이션의 advice 자체가 실행되지 않으므로 거부 이벤트 = 1.
    // - 재시도 게이트가 R4j Retry일 때: Retry.Metrics.getNumberOfTotalCalls()로 Retry의 advice가 실제로 실행됐는지를 본다.
    //   동시성 제한(Spring)이 바깥이면 Retry의 advice 자체가 실행되지 않으므로 0이어야 한다.

    /**
     * 대조군: 순수 R4j {@code @Retry} + {@code @Bulkhead}.
     */
    public static class PureR4jRetryBulkheadProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @Retry(name = "probeRetryBulkhead")
        @Bulkhead(name = "probeRetryBulkhead")
        public void call() {
            invocations.incrementAndGet();
        }
    }

    /**
     * 실험군 1 : @Retryable(바깥 선언) + R4j @Bulkhead(안쪽 선언)
     */
    public static class RetryableOutsideBulkheadProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @Retryable(includes = BulkheadFullException.class, maxRetries = 2, delay = 10)
        @Bulkhead(name = "probeCrossRetryableBulkheadFwd")
        public void call() {
            invocations.incrementAndGet();
        }
    }

    /**
     * 실험군 2 : R4j @Bulkhead(바깥 선언) + @Retryable(안쪽 선언)
     */
    public static class BulkheadOutsideRetryableProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @Bulkhead(name = "probeCrossRetryableBulkheadRev")
        @Retryable(includes = BulkheadFullException.class, maxRetries = 2, delay = 10)
        public void call() {
            invocations.incrementAndGet();
        }
    }

    /**
     * 실험군 1 : @ConcurrencyLimit(바깥 선언) + R4j @Retry(안쪽 선언)
     */
    public static class ConcurrencyLimitOutsideRetryProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @ConcurrencyLimit(value = 0, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
        @Retry(name = "probeCrossConcurrencyRetryFwd")
        public void call() {
            invocations.incrementAndGet();
        }
    }

    /**
     * 실험군 2 : R4j @Retry(바깥 선언) + @ConcurrencyLimit(안쪽 선언)
     */
    public static class RetryOutsideConcurrencyLimitProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @Retry(name = "probeCrossConcurrencyRetryRev")
        @ConcurrencyLimit(value = 0, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
        public void call() {
            invocations.incrementAndGet();
        }
    }

    // ==================== 서킷브레이커 × 동시성 제한 ====================
    // 판별 원리: 동시성 게이트가 항상 거부하도록(허용량 0) 만든 뒤, R4j CircuitBreaker가 이 거부를 "자신이
    // 겪은 실패 호출"로 기록했는지를 CircuitBreaker.Metrics.getNumberOfFailedCalls()로 확인한다.
    // CircuitBreaker가 바깥이면 자신이 직접 호출을 시도하다 실패를 겪은 것이므로 기록에 남고(=1),
    // 동시성 게이트가 바깥이면 CircuitBreaker의 advice 자체가 실행되지 않으므로 기록이 없다(=0).

    /**
     * 대조군: 순수 R4j {@code @CircuitBreaker} + {@code @Bulkhead}.
     */
    public static class PureR4jCircuitBreakerBulkheadProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @CircuitBreaker(name = "probeCbBulkhead")
        @Bulkhead(name = "probeCbBulkhead")
        public void call() {
            invocations.incrementAndGet();
        }
    }

    /**
     * 실험군 1 : @ConcurrencyLimit(바깥 선언) + R4j @CircuitBreaker(안쪽 선언)
     */
    public static class ConcurrencyLimitOutsideCircuitBreakerProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @ConcurrencyLimit(value = 0, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
        @CircuitBreaker(name = "probeCrossConcurrencyCbFwd")
        public void call() {
            invocations.incrementAndGet();
        }
    }

    /**
     * 실험군 2 : R4j @CircuitBreaker(바깥 선언) + @ConcurrencyLimit(안쪽 선언)
     */
    public static class CircuitBreakerOutsideConcurrencyLimitProbe {
        static final AtomicInteger invocations = new AtomicInteger();

        @CircuitBreaker(name = "probeCrossConcurrencyCbRev")
        @ConcurrencyLimit(value = 0, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
        public void call() {
            invocations.incrementAndGet();
        }
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        CircuitBreakerAutoConfiguration.class,
                        RetryAutoConfiguration.class,
                        BulkheadAutoConfiguration.class))
                .withUserConfiguration(ResilientMethodsTestConfig.class);
    }

    private static List<String> dumpAdvisorOrder(Object bean) {
        if (!AopUtils.isAopProxy(bean)) {
            return List.of("(프록시 아님 - 어떤 advisor도 적용되지 않음)");
        }
        Advised advised = (Advised) bean;
        return java.util.Arrays.stream(advised.getAdvisors())
                .map(a -> a.getClass().getName() + (a instanceof Ordered o ? " / order=" + o.getOrder() : ""))
                .toList();
    }

    @Test
    @DisplayName("[재시도×서킷브레이커 대조군] 순수 R4j @Retry+@CircuitBreaker: Retry가 바깥 → CB OPEN 이후 target 재호출 없음")
    void pureResilience4j_retryWrapsCircuitBreaker() {
        baseRunner()
                .withPropertyValues(
                        "resilience4j.circuitbreaker.instances.probeRetryCb.sliding-window-type=COUNT_BASED",
                        "resilience4j.circuitbreaker.instances.probeRetryCb.sliding-window-size=1",
                        "resilience4j.circuitbreaker.instances.probeRetryCb.minimum-number-of-calls=1",
                        "resilience4j.circuitbreaker.instances.probeRetryCb.failure-rate-threshold=1",
                        "resilience4j.circuitbreaker.instances.probeRetryCb.wait-duration-in-open-state=60s",
                        "resilience4j.circuitbreaker.instances.probeRetryCb.automatic-transition-from-open-to-half-open-enabled=false",
                        "resilience4j.retry.instances.probeRetryCb.max-attempts=3",
                        "resilience4j.retry.instances.probeRetryCb.wait-duration=10ms")
                .withBean("probe", PureR4jRetryCircuitBreakerProbe.class)
                .run(context -> {
                    PureR4jRetryCircuitBreakerProbe probe = context.getBean("probe", PureR4jRetryCircuitBreakerProbe.class);
                    System.out.println("[retry-cb pure] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(CallNotPermittedException.class);
                    assertThat(probe.invocations.get())
                            .as("Retry가 바깥이면 CB OPEN 이후 target이 다시 호출되지 않아야 한다")
                            .isEqualTo(1);
                });
    }

    @Test
    @DisplayName("[R4j 전용 순서 제어] *-aspect-order 프로퍼티로 기본 순서(Retry 바깥)를 CircuitBreaker 바깥으로 뒤집을 수 있다")
    void r4jAspectOrderProperty_reversesDefaultNesting() {
        baseRunner()
                .withPropertyValues(
                        // 기본값(retryAspectOrder=LOWEST_PRECEDENCE-5 < circuitBreakerAspectOrder=LOWEST_PRECEDENCE-4)이면
                        // Retry가 바깥이다(대조군 테스트에서 확인). 여기서는 두 값을 역전시켜(retry=100 > cb=50) CircuitBreaker를
                        // 더 바깥으로 만든다.
                        "resilience4j.retry.retry-aspect-order=100",
                        "resilience4j.circuitbreaker.circuit-breaker-aspect-order=50",
                        "resilience4j.circuitbreaker.instances.probeReversedOrder.sliding-window-type=COUNT_BASED",
                        "resilience4j.circuitbreaker.instances.probeReversedOrder.sliding-window-size=1",
                        "resilience4j.circuitbreaker.instances.probeReversedOrder.minimum-number-of-calls=1",
                        "resilience4j.circuitbreaker.instances.probeReversedOrder.failure-rate-threshold=1",
                        "resilience4j.circuitbreaker.instances.probeReversedOrder.wait-duration-in-open-state=60s",
                        "resilience4j.circuitbreaker.instances.probeReversedOrder.automatic-transition-from-open-to-half-open-enabled=false",
                        "resilience4j.retry.instances.probeReversedOrder.max-attempts=3",
                        "resilience4j.retry.instances.probeReversedOrder.wait-duration=10ms")
                .withBean("probe", ReversedAspectOrderProbe.class)
                .run(context -> {
                    ReversedAspectOrderProbe probe = context.getBean("probe", ReversedAspectOrderProbe.class);
                    System.out.println("[aspect-order reversed] advisors = " + dumpAdvisorOrder(probe));

                    // CircuitBreaker가 바깥이면 재시도 전체가 CB 입장에서 "호출 1건"이므로, 재시도 예산을 전부
                    // 소진할 때까지 target이 매번 실제로 호출된다(대조군과 반대 패턴).
                    assertThatThrownBy(probe::call).isInstanceOf(ProbeException.class);
                    assertThat(probe.invocations.get())
                            .as("*-aspect-order로 CircuitBreaker를 Retry보다 바깥에 두면 기본 순서가 실제로 뒤집혀야 한다")
                            .isEqualTo(3);
                });
    }

    @Test
    @DisplayName("[재시도×서킷브레이커 실험군] @Retryable(Spring, 바깥 선언) + @CircuitBreaker(R4j, 안쪽 선언)")
    void mixed_retryableDeclaredOutside_circuitBreakerDeclaredInside() {
        baseRunner()
                .withPropertyValues(
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbFwd.sliding-window-type=COUNT_BASED",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbFwd.sliding-window-size=1",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbFwd.minimum-number-of-calls=1",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbFwd.failure-rate-threshold=1",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbFwd.wait-duration-in-open-state=60s",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbFwd.automatic-transition-from-open-to-half-open-enabled=false")
                .withBean("probe", RetryableOutsideCircuitBreakerProbe.class)
                .run(context -> {
                    RetryableOutsideCircuitBreakerProbe probe =
                            context.getBean("probe", RetryableOutsideCircuitBreakerProbe.class);
                    System.out.println("[retry-cb fwd] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(CallNotPermittedException.class);
                    assertThat(probe.invocations.get())
                            .as("@Retryable(Spring)이 바깥이면 CB OPEN 이후 target 재호출이 없다")
                            .isEqualTo(1);
                });
    }

    @Test
    @DisplayName("[재시도×서킷브레이커 실험군] @CircuitBreaker(R4j, 바깥 선언) + @Retryable(Spring, 안쪽 선언) — 나열 순서 반전")
    void mixed_circuitBreakerDeclaredOutside_retryableDeclaredInside() {
        baseRunner()
                .withPropertyValues(
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbRev.sliding-window-type=COUNT_BASED",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbRev.sliding-window-size=1",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbRev.minimum-number-of-calls=1",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbRev.failure-rate-threshold=1",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbRev.wait-duration-in-open-state=60s",
                        "resilience4j.circuitbreaker.instances.probeCrossRetryableCbRev.automatic-transition-from-open-to-half-open-enabled=false")
                .withBean("probe", CircuitBreakerOutsideRetryableProbe.class)
                .run(context -> {
                    CircuitBreakerOutsideRetryableProbe probe =
                            context.getBean("probe", CircuitBreakerOutsideRetryableProbe.class);
                    System.out.println("[retry-cb rev] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(CallNotPermittedException.class);
                    assertThat(probe.invocations.get()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("[재시도×동시성 대조군] 순수 R4j @Retry+@Bulkhead: Retry가 바깥 → Bulkhead를 재시도 횟수만큼 다시 두드림")
    void pureResilience4j_retryWrapsBulkhead() {
        baseRunner()
                .withPropertyValues(
                        // max-concurrent-calls는 Spring Boot 프로퍼티 바인딩 레벨에서 >=1 검증이 있어 0을 못 받는다
                        // (핵심 BulkheadConfig.Builder 자체는 >=0 허용 — 로컬 소스로 확인). 여기 값(1)은 아래
                        // BulkheadConfigCustomizer가 그대로 덮어써서 최종 0이 되므로 실제로는 쓰이지 않는다 —
                        // 이 프로퍼티가 있어야 Spring Boot가 이 인스턴스 이름을 "알고" 있어 커스터마이저를 적용한다.
                        "resilience4j.bulkhead.instances.probeRetryBulkhead.max-concurrent-calls=1",
                        "resilience4j.retry.instances.probeRetryBulkhead.max-attempts=3",
                        "resilience4j.retry.instances.probeRetryBulkhead.wait-duration=10ms")
                .withBean("bulkheadCustomizer", BulkheadConfigCustomizer.class,
                        () -> BulkheadConfigCustomizer.of("probeRetryBulkhead", b -> b.maxConcurrentCalls(0)))
                .withBean("probe", PureR4jRetryBulkheadProbe.class)
                .run(context -> {
                    PureR4jRetryBulkheadProbe probe = context.getBean("probe", PureR4jRetryBulkheadProbe.class);
                    io.github.resilience4j.bulkhead.Bulkhead bulkhead = context.getBean(BulkheadRegistry.class).bulkhead("probeRetryBulkhead");
                    AtomicInteger rejectedCount = new AtomicInteger();
                    bulkhead.getEventPublisher().onCallRejected(e -> rejectedCount.incrementAndGet());
                    System.out.println("[retry-bulkhead pure] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(BulkheadFullException.class);
                    assertThat(probe.invocations.get())
                            .as("Bulkhead 허용량이 0이므로 target은 절대 호출되지 않아야 한다")
                            .isZero();
                    assertThat(rejectedCount.get())
                            .as("Retry가 바깥이면 재시도할 때마다 Bulkhead를 다시 두드리므로 거부 횟수 = 총 시도 횟수(3)")
                            .isEqualTo(3);
                });
    }

    @Test
    @DisplayName("[재시도×동시성 실험군] @Retryable(Spring, 바깥 선언) + @Bulkhead(R4j, 안쪽 선언)")
    void mixed_retryableDeclaredOutside_bulkheadDeclaredInside() {
        baseRunner()
                .withPropertyValues("resilience4j.bulkhead.instances.probeCrossRetryableBulkheadFwd.max-concurrent-calls=1")
                .withBean("bulkheadCustomizer", BulkheadConfigCustomizer.class,
                        () -> BulkheadConfigCustomizer.of("probeCrossRetryableBulkheadFwd", b -> b.maxConcurrentCalls(0)))
                .withBean("probe", RetryableOutsideBulkheadProbe.class)
                .run(context -> {
                    RetryableOutsideBulkheadProbe probe = context.getBean("probe", RetryableOutsideBulkheadProbe.class);
                    io.github.resilience4j.bulkhead.Bulkhead bulkhead = context.getBean(BulkheadRegistry.class).bulkhead("probeCrossRetryableBulkheadFwd");
                    AtomicInteger rejectedCount = new AtomicInteger();
                    bulkhead.getEventPublisher().onCallRejected(e -> rejectedCount.incrementAndGet());
                    System.out.println("[retry-bulkhead fwd] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(BulkheadFullException.class);
                    assertThat(probe.invocations.get()).isZero();
                    assertThat(rejectedCount.get())
                            .as("@Retryable(Spring)이 바깥이면 재시도할 때마다 Bulkhead를 다시 두드리므로 거부 횟수 = maxRetries+1(3)")
                            .isEqualTo(3);
                });
    }

    @Test
    @DisplayName("[재시도×동시성 실험군] @Bulkhead(R4j, 바깥 선언) + @Retryable(Spring, 안쪽 선언) — 나열 순서 반전")
    void mixed_bulkheadDeclaredOutside_retryableDeclaredInside() {
        baseRunner()
                .withPropertyValues("resilience4j.bulkhead.instances.probeCrossRetryableBulkheadRev.max-concurrent-calls=1")
                .withBean("bulkheadCustomizer", BulkheadConfigCustomizer.class,
                        () -> BulkheadConfigCustomizer.of("probeCrossRetryableBulkheadRev", b -> b.maxConcurrentCalls(0)))
                .withBean("probe", BulkheadOutsideRetryableProbe.class)
                .run(context -> {
                    BulkheadOutsideRetryableProbe probe = context.getBean("probe", BulkheadOutsideRetryableProbe.class);
                    io.github.resilience4j.bulkhead.Bulkhead bulkhead = context.getBean(BulkheadRegistry.class).bulkhead("probeCrossRetryableBulkheadRev");
                    AtomicInteger rejectedCount = new AtomicInteger();
                    bulkhead.getEventPublisher().onCallRejected(e -> rejectedCount.incrementAndGet());
                    System.out.println("[retry-bulkhead rev] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(BulkheadFullException.class);
                    assertThat(probe.invocations.get()).isZero();
                    assertThat(rejectedCount.get()).isEqualTo(3);
                });
    }

    @Test
    @DisplayName("[재시도×동시성 실험군] @ConcurrencyLimit(Spring, 바깥 선언) + @Retry(R4j, 안쪽 선언)")
    void mixed_concurrencyLimitDeclaredOutside_retryDeclaredInside() {
        baseRunner()
                .withBean("probe", ConcurrencyLimitOutsideRetryProbe.class)
                .run(context -> {
                    ConcurrencyLimitOutsideRetryProbe probe = context.getBean("probe", ConcurrencyLimitOutsideRetryProbe.class);
                    io.github.resilience4j.retry.Retry retry = context.getBean(RetryRegistry.class).retry("probeCrossConcurrencyRetryFwd");
                    System.out.println("[concurrency-retry fwd] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(InvocationRejectedException.class);
                    assertThat(probe.invocations.get()).isZero();
                    assertThat(retry.getMetrics().getNumberOfTotalCalls())
                            .as("@ConcurrencyLimit(Spring)이 바깥이면 R4j Retry의 advice 자체가 실행되지 않아야 한다")
                            .isZero();
                });
    }

    @Test
    @DisplayName("[재시도×동시성 실험군] @Retry(R4j, 바깥 선언) + @ConcurrencyLimit(Spring, 안쪽 선언) — 나열 순서 반전")
    void mixed_retryDeclaredOutside_concurrencyLimitDeclaredInside() {
        baseRunner()
                .withBean("probe", RetryOutsideConcurrencyLimitProbe.class)
                .run(context -> {
                    RetryOutsideConcurrencyLimitProbe probe = context.getBean("probe", RetryOutsideConcurrencyLimitProbe.class);
                    io.github.resilience4j.retry.Retry retry = context.getBean(RetryRegistry.class).retry("probeCrossConcurrencyRetryRev");
                    System.out.println("[concurrency-retry rev] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(InvocationRejectedException.class);
                    assertThat(probe.invocations.get()).isZero();
                    assertThat(retry.getMetrics().getNumberOfTotalCalls()).isZero();
                });
    }

    @Test
    @DisplayName("[서킷×동시성 대조군] 순수 R4j @CircuitBreaker+@Bulkhead: CB가 바깥 → Bulkhead 거부를 자신의 실패로 기록")
    void pureResilience4j_circuitBreakerWrapsBulkhead() {
        baseRunner()
                .withPropertyValues("resilience4j.bulkhead.instances.probeCbBulkhead.max-concurrent-calls=1")
                .withBean("bulkheadCustomizer", BulkheadConfigCustomizer.class,
                        () -> BulkheadConfigCustomizer.of("probeCbBulkhead", b -> b.maxConcurrentCalls(0)))
                .withBean("probe", PureR4jCircuitBreakerBulkheadProbe.class)
                .run(context -> {
                    PureR4jCircuitBreakerBulkheadProbe probe = context.getBean("probe", PureR4jCircuitBreakerBulkheadProbe.class);
                    io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = context.getBean(CircuitBreakerRegistry.class).circuitBreaker("probeCbBulkhead");
                    System.out.println("[cb-bulkhead pure] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(BulkheadFullException.class);
                    assertThat(probe.invocations.get()).isZero();
                    assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls())
                            .as("CircuitBreaker가 바깥이면 Bulkhead 거부를 CB 자신의 실패 호출로 기록해야 한다")
                            .isEqualTo(1);
                });
    }

    @Test
    @DisplayName("[서킷×동시성 실험군] @ConcurrencyLimit(Spring, 바깥 선언) + @CircuitBreaker(R4j, 안쪽 선언)")
    void mixed_concurrencyLimitDeclaredOutside_circuitBreakerDeclaredInside() {
        baseRunner()
                .withBean("probe", ConcurrencyLimitOutsideCircuitBreakerProbe.class)
                .run(context -> {
                    ConcurrencyLimitOutsideCircuitBreakerProbe probe =
                            context.getBean("probe", ConcurrencyLimitOutsideCircuitBreakerProbe.class);
                    io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
                            context.getBean(CircuitBreakerRegistry.class).circuitBreaker("probeCrossConcurrencyCbFwd");
                    System.out.println("[concurrency-cb fwd] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(InvocationRejectedException.class);
                    assertThat(probe.invocations.get()).isZero();
                    assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls())
                            .as("@ConcurrencyLimit(Spring)이 바깥이면 R4j CircuitBreaker의 advice 자체가 실행되지 않아야 한다")
                            .isZero();
                });
    }

    @Test
    @DisplayName("[서킷×동시성 실험군] @CircuitBreaker(R4j, 바깥 선언) + @ConcurrencyLimit(Spring, 안쪽 선언) — 나열 순서 반전")
    void mixed_circuitBreakerDeclaredOutside_concurrencyLimitDeclaredInside() {
        baseRunner()
                .withBean("probe", CircuitBreakerOutsideConcurrencyLimitProbe.class)
                .run(context -> {
                    CircuitBreakerOutsideConcurrencyLimitProbe probe =
                            context.getBean("probe", CircuitBreakerOutsideConcurrencyLimitProbe.class);
                    io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
                            context.getBean(CircuitBreakerRegistry.class).circuitBreaker("probeCrossConcurrencyCbRev");
                    System.out.println("[concurrency-cb rev] advisors = " + dumpAdvisorOrder(probe));

                    assertThatThrownBy(probe::call).isInstanceOf(InvocationRejectedException.class);
                    assertThat(probe.invocations.get()).isZero();
                    assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
                });
    }
}
