package site.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GenerateMacRequest;
import software.amazon.awssdk.services.kms.model.GenerateMacResponse;
import software.amazon.awssdk.services.kms.model.MacAlgorithmSpec;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsKmsMacHasher")
class AwsKmsMacHasherTest {

    @Mock
    private KmsClient kmsClient;

    private final KmsProperties properties = new KmsProperties("ap-northeast-2", "test-key-id", "test-hmac-key-id");

    @Test
    @DisplayName("암호화 키가 아니라 hmac 전용 키로 HMAC_SHA_256을 요청하고 결과를 Base64로 인코딩해서 돌려준다")
    void hash는_hmac_key로_HMAC_SHA_256을_요청한다() {
        final AwsKmsMacHasher hasher = new AwsKmsMacHasher(kmsClient, properties);
        final byte[] macBytes = {9, 8, 7, 6};
        given(kmsClient.generateMac(any(GenerateMacRequest.class))).willReturn(
            GenerateMacResponse.builder().mac(SdkBytes.fromByteArray(macBytes)).build());

        final String result = hasher.hash("010-1234-5678");

        assertThat(result).isEqualTo(Base64.getEncoder().encodeToString(macBytes));

        final ArgumentCaptor<GenerateMacRequest> captor = ArgumentCaptor.forClass(GenerateMacRequest.class);
        verify(kmsClient).generateMac(captor.capture());
        assertThat(captor.getValue().keyId()).isEqualTo("test-hmac-key-id");
        assertThat(captor.getValue().macAlgorithm()).isEqualTo(MacAlgorithmSpec.HMAC_SHA_256);
        assertThat(captor.getValue().message().asUtf8String()).isEqualTo("010-1234-5678");
    }
}
