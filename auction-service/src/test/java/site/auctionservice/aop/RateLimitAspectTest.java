package site.auctionservice.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.auctionservice.application.dto.PlaceBidCommand;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;
import site.auctionservice.infrastructure.ratelimit.SlidingWindowLogLimiter;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private SlidingWindowLogLimiter logLimiter;

    @InjectMocks
    private RateLimitAspect aspect;

    private static class TestTarget {
        @RateLimit(limit = 3, windowMs = 2000, keyPrefix = "bid",
                resourceIdKey = "#command.auctionId()", userIdKey = "#command.bidderId()")
        public String placeBid(PlaceBidCommand command) {
            return "raw";
        }
    }

    private ProceedingJoinPoint joinPointFor(Method method, Object... args) throws Throwable {
        MethodSignature signature = mock(MethodSignature.class);
        given(signature.getMethod()).willReturn(method);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        given(joinPoint.getSignature()).willReturn(signature);
        given(joinPoint.getArgs()).willReturn(args);
        return joinPoint;
    }

    @Test
    @DisplayName("한도 내 요청이면 'auction:antibot:ratelimit:{keyPrefix}:{resourceId}:{userId}' 키로 조회해 통과시킨다")
    void around_allowed_buildsKeyAndProceeds() throws Throwable {
        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_000));
        Method method = TestTarget.class.getMethod("placeBid", PlaceBidCommand.class);
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        ProceedingJoinPoint joinPoint = joinPointFor(method, command);
        given(joinPoint.proceed()).willReturn("raw");
        given(logLimiter.isAllowed("auction:antibot:ratelimit:bid:1:2", 3, 2000L)).willReturn(true);

        Object result = aspect.around(joinPoint, annotation);

        assertThat(result).isEqualTo("raw");
        verify(logLimiter).isAllowed("auction:antibot:ratelimit:bid:1:2", 3, 2000L);
    }

    @Test
    @DisplayName("한도를 초과하면 TooManyBidRequests 예외를 던지고 실제 로직을 실행하지 않는다")
    void around_notAllowed_throwsAndSkipsProceed() throws Throwable {
        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_000));
        Method method = TestTarget.class.getMethod("placeBid", PlaceBidCommand.class);
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        ProceedingJoinPoint joinPoint = joinPointFor(method, command);
        given(logLimiter.isAllowed("auction:antibot:ratelimit:bid:1:2", 3, 2000L)).willReturn(false);

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.TOO_MANY_BID_REQUESTS);

        verify(joinPoint, never()).proceed();
    }
}
