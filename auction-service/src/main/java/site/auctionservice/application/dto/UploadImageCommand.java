package site.auctionservice.application.dto;

public record UploadImageCommand(String contentType, long contentLength) {
}
