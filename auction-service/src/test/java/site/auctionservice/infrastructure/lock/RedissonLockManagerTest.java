package site.auctionservice.infrastructure.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import site.auctionservice.exception.ConcurrentLockException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedissonLockManagerTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private RedissonLockManager lockManager;

    @Test
    @DisplayName("락 획득에 성공하면 액션을 실행하고 finally에서 unlock한다")
    void executeWithLock_success() throws InterruptedException {
        given(redissonClient.getLock("auction:lock:1")).willReturn(rLock);
        given(rLock.tryLock(3L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        String result = lockManager.executeWithLock(
                "auction:lock:1", 3L, -1L, TimeUnit.MILLISECONDS, () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("leaseTime이 양수면 leaseTime을 명시한 tryLock을 사용한다")
    void executeWithLock_positiveLeaseTime_usesExplicitLease() throws InterruptedException {
        given(redissonClient.getLock("auction:lock:1")).willReturn(rLock);
        given(rLock.tryLock(3L, 10L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        lockManager.executeWithLock("auction:lock:1", 3L, 10L, TimeUnit.MILLISECONDS, () -> "ok");

        verify(rLock).tryLock(3L, 10L, TimeUnit.MILLISECONDS);
        verify(rLock, never()).tryLock(anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("락 획득에 실패하면 ConcurrentLockException을 던지고 unlock을 호출하지 않는다")
    void executeWithLock_notAcquired_throwsAndSkipsUnlock() throws InterruptedException {
        given(redissonClient.getLock("auction:lock:1")).willReturn(rLock);
        given(rLock.tryLock(3L, TimeUnit.MILLISECONDS)).willReturn(false);

        assertThatThrownBy(() ->
                lockManager.executeWithLock("auction:lock:1", 3L, -1L, TimeUnit.MILLISECONDS, () -> "ok"))
                .isInstanceOf(ConcurrentLockException.class);

        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("tryLock 도중 인터럽트되면 ConcurrentLockException을 던지고 인터럽트 상태를 복원한다")
    void executeWithLock_interrupted_throwsAndRestoresInterruptFlag() throws InterruptedException {
        given(redissonClient.getLock("auction:lock:1")).willReturn(rLock);
        given(rLock.tryLock(3L, TimeUnit.MILLISECONDS)).willThrow(new InterruptedException());

        assertThatThrownBy(() ->
                lockManager.executeWithLock("auction:lock:1", 3L, -1L, TimeUnit.MILLISECONDS, () -> "ok"))
                .isInstanceOf(ConcurrentLockException.class);

        assertThat(Thread.interrupted()).isTrue();
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("액션이 예외를 던져도 락은 해제된다")
    void executeWithLock_actionThrows_stillUnlocks() throws InterruptedException {
        given(redissonClient.getLock("auction:lock:1")).willReturn(rLock);
        given(rLock.tryLock(3L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        assertThatThrownBy(() ->
                lockManager.executeWithLock("auction:lock:1", 3L, -1L, TimeUnit.MILLISECONDS, () -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        verify(rLock).unlock();
    }
}
