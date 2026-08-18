package site.auctionservice.application.port.dto;

/**
 * uploadUrl: 서명된 S3 PUT 주소(유효시간 있음). imageUrl: 업로드가 끝나면 접근 가능한 공개 주소(등록에 담는 값).
 */
public record PresignedUpload(String uploadUrl, String imageUrl) {
}
