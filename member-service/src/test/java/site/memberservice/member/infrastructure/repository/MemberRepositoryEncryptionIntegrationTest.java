package site.memberservice.member.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import site.memberservice.member.domain.Address;
import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.PhoneNumber;
import site.memberservice.member.domain.repository.MemberRepository;

/**
 * name/detailAddress에 건 EncryptedStringConverter가 실제로 DB 컬럼은 암호화하고,
 * 애플리케이션에서 다시 읽을 때는 평문으로 투명하게 복호화하는지 실제 DB로 확인한다.
 */
@Disabled("DB 암/복호화 테스트가 필요하면 실행")
@Tag("integration")
@SpringBootTest
@DisplayName("Member 암호화 필드 연동")
class MemberRepositoryEncryptionIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long savedMemberId;

    @AfterEach
    void tearDown() {
        if (savedMemberId != null) {
            jdbcTemplate.update("DELETE FROM member WHERE id = ?", savedMemberId);
        }
    }

    @Test
    @DisplayName("name/detailAddress는 DB에는 암호문으로 저장되고, 리포지토리로 읽으면 평문으로 돌아온다")
    void name과_detailAddress는_DB에서는_암호문이고_조회하면_평문이다() {
        // given
        final String plainName = "홍길동";
        final String plainDetailAddress = "4층(서초동, 명정빌딩)";
        final int unique = (int) (System.nanoTime() % 9000);
        final Member member = Member.create(
            new Email("kms-test-" + System.nanoTime() + "@email.com"),
            "hashed-password-value",
            "k" + unique,
            plainName,
            new PhoneNumber("010-" + (1000 + unique) + "-5678"),
            new Address("06671", "서울특별시 서초구 반포대로 45", plainDetailAddress)
        );

        // when
        final Member saved = memberRepository.save(member);
        savedMemberId = saved.getId();

        // then — DB 원본 컬럼은 암호문이라 평문과 달라야 한다
        final Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT name, detail_address FROM member WHERE id = ?", savedMemberId);
        assertThat(row.get("name")).isNotEqualTo(plainName);
        assertThat(row.get("detail_address")).isNotEqualTo(plainDetailAddress);

        // then — 리포지토리로 다시 읽으면 평문으로 복호화돼 있어야 한다
        final Member found = memberRepository.findById(savedMemberId).orElseThrow();
        assertThat(found.getName()).isEqualTo(plainName);
        assertThat(found.getAddress().getDetailAddress()).isEqualTo(plainDetailAddress);
    }
}
