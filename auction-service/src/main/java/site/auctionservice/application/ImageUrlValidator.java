package site.auctionservice.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.auctionservice.config.S3ImageProperties;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

/**
 * 등록·수정 요청의 이미지 주소가 우리 저장소에서 발급된 것인지 확인한다.
 * 아무 주소나 허용하면 남의 서버 이미지를 우리 화면에 끌어오거나 base64가 다시 DB로 들어올 수 있다.
 */
@Component
@RequiredArgsConstructor
public class ImageUrlValidator {

    private final S3ImageProperties properties;

    public void validate(List<String> imageUrls) {
        if (imageUrls == null) {
            return;
        }
        // 슬래시까지 붙여 비교한다 — 프리픽스 문자열로 시작하는 척하는 다른 도메인(…amazonaws.com.evil.com) 차단
        String allowedPrefix = properties.publicUrlPrefix() + "/";
        for (String url : imageUrls) {
            if (url == null || !url.startsWith(allowedPrefix)) {
                throw new AuctionException(AuctionErrorCode.IMAGE_URL_NOT_ALLOWED);
            }
        }
    }
}
