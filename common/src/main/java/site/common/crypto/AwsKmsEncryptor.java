package site.common.crypto;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

import java.util.Base64;

@Slf4j
public class AwsKmsEncryptor implements KmsEncryptor {

    private final KmsClient kmsClient;
    private final KmsProperties properties;

    public AwsKmsEncryptor(final KmsClient kmsClient, final KmsProperties properties) {
        log.info("실제 KMS를 연동하는 클라이언트 빈 객체 생성");
        this.kmsClient = kmsClient;
        this.properties = properties;
    }

    @Override
    public String encrypt(final String plaintext) {
        final EncryptResponse response = kmsClient.encrypt(EncryptRequest.builder()
            .keyId(properties.keyId())
            .plaintext(SdkBytes.fromUtf8String(plaintext))
            .build());

        return Base64.getEncoder().encodeToString(response.ciphertextBlob().asByteArray());
    }

    @Override
    public String decrypt(final String ciphertext) {
        final DecryptResponse response = kmsClient.decrypt(DecryptRequest.builder()
            .ciphertextBlob(SdkBytes.fromByteArray(Base64.getDecoder().decode(ciphertext)))
            .build());

        return response.plaintext().asUtf8String();
    }
}
