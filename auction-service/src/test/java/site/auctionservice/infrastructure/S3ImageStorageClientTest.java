package site.auctionservice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.auctionservice.application.port.dto.PresignedUpload;
import site.auctionservice.config.S3ImageProperties;
import site.auctionservice.infrastructure.client.S3ImageStorageClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class S3ImageStorageClientTest {

    private final S3Presigner s3Presigner = mock(S3Presigner.class);
    private final S3ImageStorageClient storage = new S3ImageStorageClient(s3Presigner,
            new S3ImageProperties("test-bucket", "https://test-bucket.s3.ap-northeast-2.amazonaws.com"));

    @Test
    @DisplayName("발급하면_버킷과_키와_타입과_크기를_서명_조건에_담고_업로드용과_공개용_주소_쌍을_돌려준다")
    void issueUploadUrl_success() throws MalformedURLException {
        // given
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://signed.example.com/put?sig=abc").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        // when
        PresignedUpload result = storage.issueUploadUrl("auctions/abc.jpg", "image/jpeg", 1234L);

        // then
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().putObjectRequest().key()).isEqualTo("auctions/abc.jpg");
        assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/jpeg");
        assertThat(captor.getValue().putObjectRequest().contentLength()).isEqualTo(1234L);
        assertThat(result.uploadUrl()).isEqualTo("https://signed.example.com/put?sig=abc");
        assertThat(result.imageUrl())
                .isEqualTo("https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/abc.jpg");
    }
}
