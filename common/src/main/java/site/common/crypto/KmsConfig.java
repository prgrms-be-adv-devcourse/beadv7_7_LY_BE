package site.common.crypto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
@EnableConfigurationProperties(KmsProperties.class)
@ConditionalOnExpression("'${app.aws.kms.key-id:}'.length() > 0")
public class KmsConfig {

    @Bean
    public KmsClient kmsClient(final KmsProperties properties) {
        return KmsClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }

    @Bean
    public KmsEncryptor kmsEncryptor(final KmsClient kmsClient, final KmsProperties properties) {
        return new AwsKmsEncryptor(kmsClient, properties);
    }

    @Bean
    @ConditionalOnExpression("'${app.aws.kms.hmac-key-id:}'.length() > 0")
    public KmsMacHasher kmsMacHasher(final KmsClient kmsClient, final KmsProperties properties) {
        return new AwsKmsMacHasher(kmsClient, properties);
    }
}
