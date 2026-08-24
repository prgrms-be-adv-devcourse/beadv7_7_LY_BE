package site.common.crypto;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GenerateMacRequest;
import software.amazon.awssdk.services.kms.model.GenerateMacResponse;
import software.amazon.awssdk.services.kms.model.MacAlgorithmSpec;

import java.util.Base64;

@Slf4j
public class AwsKmsMacHasher implements KmsMacHasher {

    private final KmsClient kmsClient;
    private final KmsProperties properties;

    public AwsKmsMacHasher(final KmsClient kmsClient, final KmsProperties properties) {
        log.info("실제 KMS를 연동하는 HMAC 해셔 빈 객체 생성");
        this.kmsClient = kmsClient;
        this.properties = properties;
    }

    @Override
    public String hash(final String plaintext) {
        final GenerateMacResponse response = kmsClient.generateMac(GenerateMacRequest.builder()
            .keyId(properties.hmacKeyId())
            .message(SdkBytes.fromUtf8String(plaintext))
            .macAlgorithm(MacAlgorithmSpec.HMAC_SHA_256)
            .build());

        return Base64.getEncoder().encodeToString(response.mac().asByteArray());
    }
}
