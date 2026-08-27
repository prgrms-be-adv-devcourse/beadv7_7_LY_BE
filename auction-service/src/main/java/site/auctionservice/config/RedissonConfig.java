package site.auctionservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    // local 프로필은 spring.data.redis.password 자체가 정의되어 있지 않아 기본값을 빈 문자열로 둔다
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    private static final String REDISSON_HOST_PREFIX = "redis://";

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(REDISSON_HOST_PREFIX + redisHost + ":" + redisPort)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(24)
                .setTimeout(3000);
        if (!redisPassword.isBlank()) {
            singleServerConfig.setPassword(redisPassword);
        }
        // leaseTime을 명시하지 않고 tryLock(waitTime, unit) 2-파라미터 오버로드를 쓰면 이 시간만큼의 TTL로 락을 걸고 살아있는 동안 계속 갱신한다(watchdog).
        // 스레드가 죽으면 갱신이 멈추고 TTL이 그대로 만료되며 락이 풀린다 — Redisson 기본값(30_000ms)과 동일하지만 암묵적 의존을 피하려 명시.
        config.setLockWatchdogTimeout(30_000L);
        return Redisson.create(config);
    }
}
