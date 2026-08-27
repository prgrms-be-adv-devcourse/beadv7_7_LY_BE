package site.auctionservice.application.port;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import site.auctionservice.common.AuctionRedisKeys;

public interface LockPort {

    /**
     * @param key      완성된 락 키(예: "auction:lock:1")
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime -1이면 구현체가 자동 연장, 양수면 명시적 만료
     */
    <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action);

    // @DistributedLock(Aspect)과 placeBid()의 수동 호출 둘 다 identifier만 넘기면 되도록 하는 편의 메서드.
    // 키 조합(AuctionRedisKeys.lockKey)은 이 메서드 하나에서만 하므로 호출부는 AuctionRedisKeys를 몰라도 된다.
    // executeWithLock과 이름을 다르게 둔 이유: 같은 이름으로 오버로드하면 String 타입 인자가 들어올 때
    // 컴파일러가 이 메서드가 아니라 더 구체적인 위 raw 메서드로 조용히 잘못 바인딩할 수 있다.
    default <T> T executeWithLockOnAuction(Object auctionId, long waitTime, long leaseTime, TimeUnit unit,
                                            Supplier<T> action) {
        return executeWithLock(AuctionRedisKeys.lockKey(auctionId), waitTime, leaseTime, unit, action);
    }

    // 어노테이션 쪽에서 waitTime만 넘기는 경우를 위한 오버로드 (자동 연장 기본 사용)
    default <T> T executeWithLock(String key, long waitTime, TimeUnit unit, Supplier<T> action) {
        return executeWithLock(key, waitTime, -1L, unit, action);
    }
}
