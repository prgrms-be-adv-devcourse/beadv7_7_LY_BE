package site.auctionservice.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.auctionservice.config.S3ImageProperties;
import site.auctionservice.exception.AuctionException;

class ImageUrlValidatorTest {

    private final ImageUrlValidator validator = new ImageUrlValidator(
            new S3ImageProperties("test-bucket", "https://test-bucket.s3.ap-northeast-2.amazonaws.com"));

    @Test
    @DisplayName("우리_버킷_URL은_통과하고_null이나_빈_목록도_통과한다")
    void validate_ownBucketOrEmpty_pass() {
        // given
        List<String> ours = List.of("https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/a.jpg");

        // when & then
        assertThatCode(() -> validator.validate(ours)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(List.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른_출처의_URL이나_base64_문자열은_거부한다")
    void validate_foreignOrBase64_throws() {
        // given
        List<String> foreign = List.of("https://evil.example.com/x.jpg");
        List<String> base64 = List.of("data:image/jpeg;base64,AAAA");

        // when & then
        assertThatThrownBy(() -> validator.validate(foreign)).isInstanceOf(AuctionException.class);
        assertThatThrownBy(() -> validator.validate(base64)).isInstanceOf(AuctionException.class);
    }

    @Test
    @DisplayName("프리픽스로_시작하는_척하는_다른_도메인은_거부한다")
    void validate_prefixSpoofing_throws() {
        // given - 우리 프리픽스 문자열 뒤에 슬래시 없이 다른 도메인을 이어붙인 주소
        List<String> spoofed = List.of("https://test-bucket.s3.ap-northeast-2.amazonaws.com.evil.com/x.jpg");

        // when & then
        assertThatThrownBy(() -> validator.validate(spoofed)).isInstanceOf(AuctionException.class);
    }
}
