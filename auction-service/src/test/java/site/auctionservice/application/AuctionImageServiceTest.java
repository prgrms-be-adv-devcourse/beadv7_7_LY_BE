package site.auctionservice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.auctionservice.application.dto.UploadImageCommand;
import site.auctionservice.application.port.ImageStoragePort;
import site.auctionservice.application.port.dto.PresignedUpload;
import site.auctionservice.exception.AuctionException;

class AuctionImageServiceTest {

    private final ImageStoragePort imageStoragePort = mock(ImageStoragePort.class);
    private final AuctionImageService service =
            new AuctionImageService(imageStoragePort, new ImageUploadValidator());

    @Test
    @DisplayName("여러_장을_요청하면_형식별_확장자의_auctions_키로_발급하고_순서대로_돌려준다")
    void issueUploadUrls_success() {
        // given
        when(imageStoragePort.issueUploadUrl(anyString(), eq("image/jpeg"), anyLong()))
                .thenReturn(new PresignedUpload("https://signed/a", "https://cdn/a.jpg"));
        when(imageStoragePort.issueUploadUrl(anyString(), eq("image/png"), anyLong()))
                .thenReturn(new PresignedUpload("https://signed/b", "https://cdn/b.png"));

        // when
        List<PresignedUpload> results = service.issueUploadUrls(List.of(
                new UploadImageCommand("image/jpeg", 1024L),
                new UploadImageCommand("image/png", 2048L)));

        // then
        assertThat(results).extracting(PresignedUpload::imageUrl)
                .containsExactly("https://cdn/a.jpg", "https://cdn/b.png");
        verify(imageStoragePort).issueUploadUrl(matches("auctions/[0-9a-f-]+\\.jpg"), eq("image/jpeg"), eq(1024L));
        verify(imageStoragePort).issueUploadUrl(matches("auctions/[0-9a-f-]+\\.png"), eq("image/png"), eq(2048L));
    }

    @Test
    @DisplayName("검증에_걸리는_파일이_한_장이라도_있으면_아무것도_발급하지_않는다")
    void issueUploadUrls_invalidFile_nothingIssued() {
        // given - 두 번째 파일이 비허용 타입
        List<UploadImageCommand> commands = List.of(
                new UploadImageCommand("image/jpeg", 1024L),
                new UploadImageCommand("application/pdf", 1024L));

        // when & then
        assertThatThrownBy(() -> service.issueUploadUrls(commands)).isInstanceOf(AuctionException.class);
        verify(imageStoragePort, never()).issueUploadUrl(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("빈_목록이면_발급_없이_빈_목록을_돌려준다")
    void issueUploadUrls_empty_returnsEmpty() {
        // given

        // when
        List<PresignedUpload> results = service.issueUploadUrls(Collections.emptyList());

        // then
        assertThat(results).isEmpty();
        verify(imageStoragePort, never()).issueUploadUrl(anyString(), anyString(), anyLong());
    }
}
