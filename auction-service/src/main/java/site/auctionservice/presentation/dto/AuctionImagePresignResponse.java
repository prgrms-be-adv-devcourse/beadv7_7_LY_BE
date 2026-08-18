package site.auctionservice.presentation.dto;

import java.util.List;
import site.auctionservice.application.port.dto.PresignedUpload;

public record AuctionImagePresignResponse(List<PresignedImage> presignedImages) {

    public record PresignedImage(String uploadUrl, String imageUrl) {
    }

    public static AuctionImagePresignResponse from(List<PresignedUpload> uploads) {
        return new AuctionImagePresignResponse(uploads.stream()
                .map(upload -> new PresignedImage(upload.uploadUrl(), upload.imageUrl()))
                .toList());
    }
}
