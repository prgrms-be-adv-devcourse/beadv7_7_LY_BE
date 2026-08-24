package site.auctionservice.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String prefix();    // 도메인,  "auction" → 최종 키: "{prefix}:lock:{key}"
    String key();       // 식별자 SpEL, 예: "#command.auctionId()"
    long waitTime() default 3L;     // 락 획득 대기 시간
    long leaseTime() default -1L;   // 락 임대 시간 : -1이면 watchdog 자동 연장
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
