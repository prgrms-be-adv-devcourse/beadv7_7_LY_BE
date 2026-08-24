package site.auctionservice.application.port;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface LockPort {

    /**
     * @param key      락 컨벤션(예: "auction:lock:1")까지 포함한 완성된 키 — 호출부(@DistributedLock)가 결정
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime -1이면 구현체가 자동 연장, 양수면 명시적 만료
     */
    <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action);

    // 어노테이션 쪽에서 waitTime만 넘기는 경우를 위한 오버로드 (자동 연장 기본 사용)
    default <T> T executeWithLock(String key, long waitTime, TimeUnit unit, Supplier<T> action) {
        return executeWithLock(key, waitTime, -1L, unit, action);
    }
}
