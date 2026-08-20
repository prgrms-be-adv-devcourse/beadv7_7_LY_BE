package site.common.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// local 프로필에서 app.aws.kms.key-id가 비어 있으면(기본값) 실제 KMS 대신 이걸 쓴다 —
// 팀원 전원이 AWS 자격 증명 없이 바로 로컬에서 애플리케이션을 실행할 수 있게 하기 위함.
// @Profile("local")이 핵심 안전장치다 — 다른 프로필(운영 등)에서는 key-id를 깜빡 잊어도
// 이 빈이 활성화될 길이 아예 없다. 그런 환경은 KmsConfig의 빈도 없어 기동이 실패하는 쪽이
// 조용히 가짜로 동작하는 것보다 안전하다.
@Slf4j
@Component
@Profile("local")
@ConditionalOnExpression("'${app.aws.kms.key-id:}'.length() == 0")
public class FakeKmsEncryptor implements KmsEncryptor {

    public FakeKmsEncryptor() {
        log.warn("app.aws.kms.key-id가 비어 있어 FakeKmsEncryptor를 사용합니다. 로컬 개발 전용이며 실제로 암호화되지 않습니다.");
    }

    @Override
    public String encrypt(final String plaintext) {
        return Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(final String ciphertext) {
        return new String(Base64.getDecoder().decode(ciphertext), StandardCharsets.UTF_8);
    }
}
