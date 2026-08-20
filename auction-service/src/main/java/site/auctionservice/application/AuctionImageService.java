package site.auctionservice.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.auctionservice.application.dto.UploadImageCommand;
import site.auctionservice.application.port.ImageStoragePort;
import site.auctionservice.application.port.dto.PresignedUpload;

@Service
@RequiredArgsConstructor
public class AuctionImageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");
    private final ImageStoragePort imageStoragePort;
    private final ImageUploadValidator imageUploadValidator;

    public List<PresignedUpload> issueUploadUrls(List<UploadImageCommand> uploadImageCommands) {
        imageUploadValidator.validate(uploadImageCommands.size());
        for (UploadImageCommand uploadImageCommand : uploadImageCommands) {
            imageUploadValidator.validate(uploadImageCommand.contentType(), uploadImageCommand.contentLength());
        }

        return uploadImageCommands.stream()
                .map(uploadImageCommand -> imageStoragePort.issueUploadUrl(
                        "auctions/" + UUID.randomUUID() + "." + EXTENSIONS.get(uploadImageCommand.contentType()),
                        uploadImageCommand.contentType(), uploadImageCommand.contentLength()))
                .toList();
    }
}
