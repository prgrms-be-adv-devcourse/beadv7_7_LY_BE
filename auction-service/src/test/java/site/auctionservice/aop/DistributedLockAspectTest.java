package site.auctionservice.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.auctionservice.infrastructure.lock.DistributedLockManager;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DistributedLockAspectTest {

    @Mock
    private DistributedLockManager lockManager;

    @InjectMocks
    private DistributedLockAspect aspect;

    private static class TestTarget {
        @DistributedLock(prefix = "auction", key = "#auctionId")
        public String doSomething(Long auctionId) {
            return "raw";
        }
    }

    @SuppressWarnings("unchecked")
    private void stubLockManagerToRunAction() {
        given(lockManager.executeWithLock(any(), anyLong(), anyLong(), any(), any()))
                .willAnswer(invocation -> {
                    Supplier<Object> action = invocation.getArgument(4);
                    return action.get();
                });
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
    @DisplayName("prefix와 SpEL로 평가한 식별자를 'prefix:lock:식별자' 형식으로 조립해 락 매니저에 넘긴다")
    void lock_buildsKeyWithPrefixConvention() throws Throwable {
        stubLockManagerToRunAction();
        Method method = TestTarget.class.getMethod("doSomething", Long.class);
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);
        ProceedingJoinPoint joinPoint = joinPointFor(method, 5L);
        given(joinPoint.proceed()).willReturn("raw");

        Object result = aspect.lock(joinPoint, annotation);

        assertThat(result).isEqualTo("raw");
        org.mockito.Mockito.verify(lockManager).executeWithLock(
                eq("auction:lock:5"), eq(3L), eq(-1L), eq(TimeUnit.MILLISECONDS), any());
    }

    @Test
    @DisplayName("타겟 메서드가 RuntimeException을 던지면 감싸지 않고 그대로 전파한다")
    void lock_targetThrowsRuntimeException_propagatesAsIs() throws Throwable {
        stubLockManagerToRunAction();
        Method method = TestTarget.class.getMethod("doSomething", Long.class);
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);
        ProceedingJoinPoint joinPoint = joinPointFor(method, 5L);
        IllegalStateException original = new IllegalStateException("boom");
        given(joinPoint.proceed()).willThrow(original);

        assertThatThrownBy(() -> aspect.lock(joinPoint, annotation))
                .isSameAs(original);
    }

    @Test
    @DisplayName("타겟 메서드가 체크 예외를 던지면 RuntimeException으로 감싸서 전파한다")
    void lock_targetThrowsCheckedException_wrapsInRuntimeException() throws Throwable {
        stubLockManagerToRunAction();
        Method method = TestTarget.class.getMethod("doSomething", Long.class);
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);
        ProceedingJoinPoint joinPoint = joinPointFor(method, 5L);
        Exception checked = new Exception("checked failure");
        given(joinPoint.proceed()).willThrow(checked);

        assertThatThrownBy(() -> aspect.lock(joinPoint, annotation))
                .isInstanceOf(RuntimeException.class)
                .hasCause(checked);
    }
}
