package site.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FakeKmsEncryptor")
class FakeKmsEncryptorTest {

    private final FakeKmsEncryptor encryptor = new FakeKmsEncryptor();

    @Test
    @DisplayName("암호화한 값은 원문과 다르고, 복호화하면 원문으로 돌아온다")
    void 암호화한_뒤_복호화하면_원문과_같다() {
        final String plaintext = "123-456-789";

        final String ciphertext = encryptor.encrypt(plaintext);
        final String decrypted = encryptor.decrypt(ciphertext);

        assertThat(ciphertext).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }
}
