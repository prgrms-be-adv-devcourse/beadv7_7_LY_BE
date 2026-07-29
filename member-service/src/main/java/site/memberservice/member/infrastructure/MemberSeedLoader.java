package site.memberservice.member.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import site.memberservice.member.domain.repository.MemberRepository;

import java.time.LocalDateTime;

/**
 * core-service AuctionSeedLoader의 SEED_SELLER_ID(111)로 로그인해볼 수 있게 미리 넣어두는 판매자 회원 시드.
 * id를 111로 강제해야 해서 IDENTITY 전략인 JPA save 대신 JdbcTemplate으로 직접 insert한다.
 * local 프로파일 + member.seed.enabled=true일 때만 동작(기본 OFF), 멱등성은 findById로 확인한다.
 */
@Slf4j
@Profile("local")
@ConditionalOnProperty(prefix = "member.seed", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class MemberSeedLoader implements CommandLineRunner {

    private static final Long SEED_SELLER_ID = 111L;
    private static final String SEED_SELLER_EMAIL = "seller@example.com";
    private static final String SEED_SELLER_PASSWORD = "seller1234!";
    private static final String SEED_SELLER_NICKNAME = "시드 판매자";

    private final MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (memberRepository.findById(SEED_SELLER_ID).isPresent()) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO member
                    (id, email, password, nickname, name, phone_number, zipcode, base_address, detail_address, createdAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                SEED_SELLER_ID,
                SEED_SELLER_EMAIL,
                passwordEncoder.encode(SEED_SELLER_PASSWORD),
                SEED_SELLER_NICKNAME,
                "시드 판매자",
                "010-1111-2222",
                "06671",
                "서울특별시 서초구 반포대로 45",
                "1층",
                LocalDateTime.now()
        );

        log.info("[MemberSeedLoader] seed member(id={}) uploaded", SEED_SELLER_ID);
    }
}
