package site.memberservice.global.crypto.hashing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Argon2Hasher 구현체")
class Argon2HasherTest {

    private static final Argon2Hasher hasher = new Argon2Hasher();

    @DisplayName("평문 비밀번호를 입력하면 Argon2 알고리즘을 사용해 해싱한다.")
    @Test
    void hashingUseArgon2() {
        // Given
        final String rowPassword = "testPw1234!";

        // When
        final String hashedPassword = hasher.hashing(rowPassword);

        // Then
        assertThat(hasher.matches(rowPassword, hashedPassword)).isTrue();
        assertThat(hasher.matches("testerPw1234@", hashedPassword)).isFalse();
    }
}
