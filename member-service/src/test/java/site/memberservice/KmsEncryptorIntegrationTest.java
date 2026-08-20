package site.memberservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import site.common.crypto.KmsEncryptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 AWS KMS와 연동해 암/복호화 왕복이 되는지 확인한다.
 * <p>
 * 실행 전제: application-local.yml의 AWS_KMS_REGION·AWS_KMS_KEY_ID를 본인 AWS 계정의 실제 값으로
 * 채우고, 그 키에 대해 kms:Encrypt·kms:Decrypt 권한을 가진 자격 증명이 로컬에 있어야 한다
 * (~/.aws/credentials 프로필 또는 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY 환경변수 —
 * DefaultCredentialsProvider가 자동으로 찾는다).
 * <p>
 * 주의: key-id를 채우지 않으면 FakeKmsEncryptor가 대신 주입돼 이 테스트가 실제 AWS 호출 없이도
 * "성공"으로 끝나버린다 — 진짜 KMS를 검증하려는 목적이라면 key-id가 꼭 채워져 있는지 확인할 것.
 */
@Disabled("실제 AWS KMS 연동 테스트가 필요하면 해당 테스트 실행")
@Tag("integration")
@SpringBootTest
@DisplayName("KMS 암복호화 연동")
class KmsEncryptorIntegrationTest {

    @Autowired
    private KmsEncryptor kmsEncryptor;

    @Test
    @DisplayName("평문을 암호화한 뒤 복호화하면 원문과 같다")
    void 암호화한_뒤_복호화하면_원문과_같다() {
        final String plaintext = "110-123-456789";

        final String ciphertext = kmsEncryptor.encrypt(plaintext);
        final String decrypted = kmsEncryptor.decrypt(ciphertext);

        assertThat(ciphertext).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }
}
