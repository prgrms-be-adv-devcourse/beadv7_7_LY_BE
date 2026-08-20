package site.auctionservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3 이미지 저장 설정. publicUrlPrefix는 업로드 주소 발급(S3ImageStorage)과 등록 시 URL 출처 확인(ImageUrlValidator) 두 곳에서 사용 — 낱개 @Value로 두면
 * 설정 키 문자열이 클래스마다 중복되고, 테스트에서 값 넣기도 번거로워 묶음 객체로 만들었다.
 */
@ConfigurationProperties(prefix = "s3.image")
public record S3ImageProperties(String bucket, String publicUrlPrefix) {
}
