package site.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws.kms")
public record KmsProperties(String region, String keyId) {
}
