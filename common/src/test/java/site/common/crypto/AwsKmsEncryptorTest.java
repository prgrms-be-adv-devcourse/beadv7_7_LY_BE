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
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsKmsEncryptor")
class AwsKmsEncryptorTest {

    @Mock
    private KmsClient kmsClient;

    private final KmsProperties properties = new KmsProperties("ap-northeast-2", "test-key-id", "test-hmac-key-id");

    @Test
    @DisplayName("암호화하면 설정된 키로 요청하고 결과를 Base64로 인코딩해서 돌려준다")
    void encrypt는_Base64로_인코딩한다() {
        final AwsKmsEncryptor encryptor = new AwsKmsEncryptor(kmsClient, properties);
        final byte[] cipherBytes = {1, 2, 3, 4};
        given(kmsClient.encrypt(any(EncryptRequest.class))).willReturn(
            EncryptResponse.builder().ciphertextBlob(SdkBytes.fromByteArray(cipherBytes)).build());

        final String result = encryptor.encrypt("123-456-789");

        assertThat(result).isEqualTo(Base64.getEncoder().encodeToString(cipherBytes));

        final ArgumentCaptor<EncryptRequest> captor = ArgumentCaptor.forClass(EncryptRequest.class);
        verify(kmsClient).encrypt(captor.capture());
        assertThat(captor.getValue().keyId()).isEqualTo("test-key-id");
        assertThat(captor.getValue().plaintext().asUtf8String()).isEqualTo("123-456-789");
    }

    @Test
    @DisplayName("복호화하면 Base64를 디코딩해서 요청하고 평문을 그대로 돌려준다")
    void decrypt는_Base64를_디코딩한다() {
        final AwsKmsEncryptor encryptor = new AwsKmsEncryptor(kmsClient, properties);
        final String ciphertext = Base64.getEncoder().encodeToString(new byte[] {5, 6, 7});
        given(kmsClient.decrypt(any(DecryptRequest.class))).willReturn(
            DecryptResponse.builder().plaintext(SdkBytes.fromUtf8String("123-456-789")).build());

        final String result = encryptor.decrypt(ciphertext);

        assertThat(result).isEqualTo("123-456-789");

        final ArgumentCaptor<DecryptRequest> captor = ArgumentCaptor.forClass(DecryptRequest.class);
        verify(kmsClient).decrypt(captor.capture());
        assertThat(captor.getValue().ciphertextBlob().asByteArray()).isEqualTo(new byte[] {5, 6, 7});
    }
}
