package site.auctionservice.application.port;

import site.auctionservice.application.port.dto.PresignedUpload;

public interface ImageStoragePort {
    /**
     * 브라우저가 파일을 직접 올릴 수 있는 서명된 업로드 주소와, 업로드 완료 후의 공개 주소를 발급한다.
     */
    PresignedUpload issueUploadUrl(String key, String contentType, long contentLength);
}
