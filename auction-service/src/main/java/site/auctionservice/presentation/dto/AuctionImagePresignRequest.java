package site.auctionservice.presentation.dto;

import java.util.List;
import site.auctionservice.application.dto.UploadImageCommand;

public record AuctionImagePresignRequest(List<FileInfo> files) {

    public record FileInfo(String contentType, Long contentLength) {
    }

    public List<UploadImageCommand> toCommands() {
        return files.stream()
                .map(file -> new UploadImageCommand(file.contentType(),
                        file.contentLength() == null ? 0L : file.contentLength()))
                .toList();
    }
}
