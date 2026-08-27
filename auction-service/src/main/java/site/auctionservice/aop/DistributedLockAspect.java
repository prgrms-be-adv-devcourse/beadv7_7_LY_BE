package site.auctionservice.aop;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import site.auctionservice.application.port.LockPort;

// @Transactional 어드바이스보다 바깥쪽에서 실행되도록 순위를 강제한다.
// 순서가 뒤바뀌면 트랜잭션 커밋 전에 락이 풀려서 다른 스레드가 변경 이전 데이터를 읽을 수 있다.
// 극단값(HIGHEST_PRECEDENCE) 대신 여유 있는 숫자를 써서, 나중에 이보다 더 바깥이어야 하는
// 애스펙트가 생기면 그 사이 값으로 끼워 넣을 수 있게 여지를 남긴다.
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final LockPort lockManager;

    private static final ExpressionParser parser = new SpelExpressionParser();
    private static final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String identifier = parseKey(joinPoint, distributedLock.key());

        return lockManager.executeWithLockOnAuction(
                identifier, distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit(),
                () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable t) {
                        throw (t instanceof RuntimeException re) ? re : new RuntimeException(t);
                    }
                }
        );
    }

    private String parseKey(ProceedingJoinPoint joinPoint, String spEL) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = discoverer.getParameterNames(signature.getMethod());
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        return parser.parseExpression(spEL).getValue(context, String.class);
    }
}
