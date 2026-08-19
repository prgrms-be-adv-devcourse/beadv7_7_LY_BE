package site.common.crypto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

// app.aws.kms.key-id가 설정된 서비스에서만 빈을 만든다.
// 아직 KMS를 쓰지 않는 서비스(auction, gateway, product 등)의 컨텍스트 로딩에 영향을 주지 않기 위함.
@Configuration
@EnableConfigurationProperties(KmsProperties.class)
@ConditionalOnProperty(prefix = "app.aws.kms", name = "key-id")
public class KmsConfig {

    @Bean
    public KmsClient kmsClient(final KmsProperties properties) {
        return KmsClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }
}
