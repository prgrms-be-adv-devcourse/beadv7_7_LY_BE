package site.memberservice.global.crypto.hashing;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2Hasher implements Hasher {

    private final Argon2PasswordEncoder argon2PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String hash(final String target) {
        return argon2PasswordEncoder.encode(target);
    }

    @Override
    public boolean match(final String rawTarget, final String hashedTarget) {
        return argon2PasswordEncoder.matches(rawTarget, hashedTarget);
    }
}
