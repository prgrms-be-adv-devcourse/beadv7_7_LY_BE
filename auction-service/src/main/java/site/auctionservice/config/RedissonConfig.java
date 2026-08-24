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
                // 락 소유 스레드가 죽었을 때 자동 해제까지 걸리는 기본 시간(watchdog)
                .setTimeout(3000);
        if (!redisPassword.isBlank()) {
            singleServerConfig.setPassword(redisPassword);
        }
        // lockWatchdogTimeout 기본값은 30_000ms. leaseTime을 명시하지 않고
        // tryLock(waitTime, unit) 2-파라미터 오버로드를 쓰면 이 watchdog이 자동 갱신을 돌려준다.
        return Redisson.create(config);
    }
}
