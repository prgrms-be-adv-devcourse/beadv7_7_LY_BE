package site.auctionservice.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit();                 // 위험 점수에 따라 더 좁게 적용될 수 있음
    long windowMs();
    String keyPrefix();          // 최종 키 조합은 AuctionRedisKeys.rateLimitKey() 참고
    String resourceIdKey();      // 대상 리소스 ID SpEL, 예: "#command.auctionId()"
    String userIdKey();          // 요청 유저 ID SpEL, 예: "#command.bidderId()"
}
