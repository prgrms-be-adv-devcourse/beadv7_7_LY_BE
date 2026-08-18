package site.auctionservice.infrastructure.client;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.auctionservice.application.port.ImageStoragePort;
import site.auctionservice.application.port.dto.PresignedUpload;
import site.auctionservice.config.S3ImageProperties;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3ImageStorageClient implements ImageStoragePort {

    /**
     * 발급된 업로드 주소의 유효 시간 — 폼 작성 중 만료되지 않을 만큼만 짧게.
     */
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);

    private final S3Presigner s3Presigner;
    private final S3ImageProperties properties;

    @Override
    public PresignedUpload issueUploadUrl(String key, String contentType, long contentLength) {

        // contentType·contentLength를 서명에 포함한다 — 브라우저가 선언과 다른 것을 올리면 S3가 거부
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_TTL)
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new PresignedUpload(uploadUrl, properties.publicUrlPrefix() + "/" + key);
    }
}
