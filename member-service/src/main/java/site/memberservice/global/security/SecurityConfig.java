package site.memberservice.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 기본 권장 설정 (Salt 16bytes, Hash 32bytes, Parallelism 1, Memory 16MB, Iterations 2)
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
