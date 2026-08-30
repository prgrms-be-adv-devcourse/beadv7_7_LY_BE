package site.auctionservice.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import site.auctionservice.common.AuctionRedisKeys;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;
import site.auctionservice.infrastructure.macro.BidRiskScoreManager;
import site.auctionservice.infrastructure.ratelimit.SlidingWindowLogLimiter;

@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class RateLimitAspect {

    private final SlidingWindowLogLimiter logLimiter;
    private final BidRiskScoreManager riskScoreManager;

    private static final ExpressionParser parser = new SpelExpressionParser();
    private static final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        StandardEvaluationContext context = evaluationContext(joinPoint);
        Long resourceId = parser.parseExpression(rateLimit.resourceIdKey()).getValue(context, Long.class);
        Long userId = parser.parseExpression(rateLimit.userIdKey()).getValue(context, Long.class);
        String key = AuctionRedisKeys.rateLimitKey(rateLimit.keyPrefix(), resourceId, userId);

        // 위험 점수가 쌓여있으면 이번 요청부터 적용할 limit을 동적으로 좁힌다.
        int effectiveLimit = riskScoreManager.adjustLimit(rateLimit.limit(), userId);
        if (!logLimiter.isAllowed(key, effectiveLimit, rateLimit.windowMs())) {
            throw new AuctionException(AuctionErrorCode.TOO_MANY_BID_REQUESTS);
        }

        return joinPoint.proceed();
    }

    private StandardEvaluationContext evaluationContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = discoverer.getParameterNames(signature.getMethod());
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        return context;
    }
}
