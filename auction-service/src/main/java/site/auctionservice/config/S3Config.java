package site.auctionservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3ImageProperties.class)
public class S3Config {

    /**
     * 자격증명은 환경변수(AWS_ACCESS_KEY_ID 등)에서 SDK 기본 탐색으로 읽는다 — 레포 public이라 코드·yml에 두지 않는다. presign 서명은 로컬 연산이라 이 빈이 S3에 네트워크
     * 요청을 보내지는 않는다.
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "ap-northeast-2")))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}
