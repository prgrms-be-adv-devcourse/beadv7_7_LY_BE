package site.auctionservice.infrastructure.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import site.auctionservice.application.port.LockPort;
import site.auctionservice.exception.LockAcquisitionFailedException;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RedissonLockManager implements LockPort {
    private final RedissonClient redissonClient;

    // leaseTime -1: Redisson watchdog이 자동 연장, 양수: 명시적 만료
    @Override
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = (leaseTime <= 0)
                    ? lock.tryLock(waitTime, unit)
                    : lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                throw new LockAcquisitionFailedException(key);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionFailedException(key, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
