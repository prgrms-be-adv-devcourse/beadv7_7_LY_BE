package site.common.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import site.common.exception.BusinessException;
import site.common.exception.GlobalErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@Component
@Profile("local")
@ConditionalOnExpression("'${app.aws.kms.hmac-key-id:}'.length() == 0")
public class FakeKmsMacHasher implements KmsMacHasher {

    public FakeKmsMacHasher() {
        log.warn("app.aws.kms.hmac-key-id가 비어 있어 FakeKmsMacHasher를 사용합니다. 로컬 개발 전용입니다.");
    }

    @Override
    public String hash(final String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new BusinessException(GlobalErrorCode.INVALID_ARGUMENT, "plaintext는 null 혹은 공백일 수 없습니다.");
        }

        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hashed = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
