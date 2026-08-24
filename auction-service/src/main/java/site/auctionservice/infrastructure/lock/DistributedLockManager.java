package site.auctionservice.infrastructure.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import site.auctionservice.exception.ConcurrentLockException;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DistributedLockManager {
    private final RedissonClient redissonClient;

    /**
     * @param key      락 컨벤션(예: "auction:lock:1")까지 포함한 완성된 키 — 호출부(@DistributedLock)가 결정
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime -1이면 watchdog 자동 연장, 양수면 명시적 만료
     */
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = (leaseTime <= 0)
                    ? lock.tryLock(waitTime, unit)
                    : lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                throw new ConcurrentLockException(key);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcurrentLockException(key, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 어노테이션 쪽에서 waitTime만 넘기는 경우를 위한 오버로드 (watchdog 기본 사용)
    public <T> T executeWithLock(String key, long waitTime, TimeUnit unit, Supplier<T> action) {
        return executeWithLock(key, waitTime, -1L, unit, action);
    }

}
