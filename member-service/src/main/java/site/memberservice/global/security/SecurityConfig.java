package site.memberservice.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.Semaphore;

@Configuration
public class SecurityConfig {

    private static final int MEMORY_KIB = 9216; // 9 MIB
    private static final int ITERATIONS = 4;
    private static final int PARALLELISM = 1;
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;

    private static final int ARGON2_MAX_CONCURRENCY = Runtime.getRuntime().availableProcessors();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KIB, ITERATIONS);
    }

    @Bean
    public Semaphore argon2ConcurrencyLimiter() {
        return new Semaphore(ARGON2_MAX_CONCURRENCY);
    }
}
