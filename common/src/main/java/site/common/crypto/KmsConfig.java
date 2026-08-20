package site.common.crypto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

// app.aws.kms.key-id가 공백이 아닌 서비스에서만 빈을 만든다. 아직 KMS를 쓰지 않는 서비스
// (auction, gateway, product 등)나, key-id를 비워둔 로컬 환경(FakeKmsEncryptor 참고)의
// 컨텍스트 로딩에 영향을 주지 않기 위함.
// @ConditionalOnProperty(name = "key-id")는 "존재 여부"만 보기 때문에 빈 문자열도 "존재"로
// 판정해버려서(실제로 KmsException으로 확인됨) 쓸 수 없다 — 반드시 공백 여부를 판정해야 한다.
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
}
