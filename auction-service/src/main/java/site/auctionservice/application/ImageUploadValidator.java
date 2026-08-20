package site.auctionservice.application;

import java.util.Set;
import org.springframework.stereotype.Component;
import site.auctionservice.domain.AuctionPolicy;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

@Component
public class ImageUploadValidator {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public void validate(int fileCount) {
        if (fileCount > AuctionPolicy.MAX_IMAGE_COUNT) {
            throw new AuctionException(AuctionErrorCode.IMAGE_COUNT_EXCEEDED);
        }
    }

    public void validate(String contentType, long contentSize) {
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new AuctionException(AuctionErrorCode.IMAGE_TYPE_INVALID);
        }
        if (contentSize <= 0 || contentSize > AuctionPolicy.MAX_IMAGE_BYTES) {
            throw new AuctionException(AuctionErrorCode.IMAGE_SIZE_EXCEEDED);
        }
    }
}
