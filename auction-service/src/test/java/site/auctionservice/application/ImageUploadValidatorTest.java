package site.auctionservice.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.auctionservice.exception.AuctionException;

class ImageUploadValidatorTest {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    @DisplayName("jpeg_png_webp_타입에_크기가_한도_이내면_통과한다")
    void validate_allowedTypeAndSize_pass() {
        // given - 경계값 포함

        // when & then
        assertThatCode(() -> validator.validate("image/jpeg", 1024L)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("image/png", MAX_BYTES)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("image/webp", 1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("허용_목록에_없는_타입은_거부한다")
    void validate_disallowedType_throws() {
        // given

        // when & then
        assertThatThrownBy(() -> validator.validate("image/gif", 1024L))
                .isInstanceOf(AuctionException.class)
                .hasMessageContaining("이미지 형식");
        assertThatThrownBy(() -> validator.validate(null, 1024L)).isInstanceOf(AuctionException.class);
    }

    @Test
    @DisplayName("한도를_넘거나_0이하인_크기는_거부한다")
    void validate_invalidSize_throws() {
        // given

        // when & then
        assertThatThrownBy(() -> validator.validate("image/jpeg", MAX_BYTES + 1))
                .isInstanceOf(AuctionException.class)
                .hasMessageContaining("크기");
        assertThatThrownBy(() -> validator.validate("image/jpeg", 0L)).isInstanceOf(AuctionException.class);
    }

    @Test
    @DisplayName("장수가_한도_이내면_통과하고_초과하면_거부한다")
    void validateCount_boundary() {
        // given - 한도는 AuctionPolicy.MAX_IMAGE_COUNT(5)

        // when & then
        assertThatCode(() -> validator.validate(5)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(6)).isInstanceOf(AuctionException.class);
    }
}
