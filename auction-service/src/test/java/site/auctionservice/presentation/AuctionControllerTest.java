package site.auctionservice.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.response.ApiResponse;
import site.auctionservice.application.AuctionService;
import site.auctionservice.application.dto.*;
import site.auctionservice.application.port.dto.ProductDetail;
import site.auctionservice.presentation.dto.AuctionDetailResponse;
import site.auctionservice.presentation.dto.AuctionRequest;
import site.auctionservice.presentation.dto.AuctionResultResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionControllerTest {

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionController auctionController;

    @Test
    @DisplayName("경매 생성 요청을 커맨드로 변환해 서비스에 위임하고 결과를 감싸 반환한다")
    void testCreateAuction_delegatesToServiceAndWrapsResult() {
        // given
        AuctionRequest request = new AuctionRequest(
                100L,
                "MINT",
                "충분히 긴 상품 설명입니다.",
                List.of("1.png"),
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(3_000),
                BigDecimal.valueOf(500),
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 2, 0, 0),
                true,
                5
        );
        given(auctionService.createAuction(any(CreateAuctionCommand.class), eq(1L))).willReturn(new AuctionResult(1L, "SCHEDULED"));

        // when
        ApiResponse<AuctionResultResponse> response = auctionController.createAuction(request, 1L);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().auctionId()).isEqualTo(1L);
        assertThat(response.getData().status()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("경매 수정 요청을 커맨드로 변환해 서비스에 위임하고 결과를 감싸 반환한다")
    void testModifyAuction_delegatesToServiceAndWrapsResult() {
        // given
        AuctionRequest request = new AuctionRequest(
                100L,
                "MINT",
                "충분히 긴 상품 설명입니다.",
                List.of("1.png"),
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(3_000),
                BigDecimal.valueOf(500),
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 2, 0, 0),
                true,
                5
        );
        given(auctionService.modifyAuction(any(ModifyAuctionCommand.class), eq(1L))).willReturn(new AuctionResult(1L, "SCHEDULED"));

        // when
        ApiResponse<AuctionResultResponse> response = auctionController.modifyAuction(request, 1L, 1L);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().auctionId()).isEqualTo(1L);
        assertThat(response.getData().status()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("경매 삭제 요청을 서비스에 위임한다")
    void testDeleteAuction_delegatesToService() {
        // when
        ApiResponse<Void> response = auctionController.deleteAuction(1L, 1L);

        // then
        assertThat(response.isSuccess()).isTrue();
        verify(auctionService).deleteAuction(1L, 1L);
    }

    private AuctionDetailResult detailResult() {
        ProductDetail product = new ProductDetail(100L, "Abbey Road", "The Beatles",
                "https://cdn.example.com/cover.jpg", 1969, "Rock", "ORIGINAL", true);
        return new AuctionDetailResult(
                1L,
                "MINT", "충분히 긴 상품 설명입니다.", List.of("1.png"),
                BigDecimal.valueOf(10_000), BigDecimal.valueOf(3_000),
                BigDecimal.valueOf(13_000), BigDecimal.valueOf(500),
                LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0),
                false, null,
                product, 1L, "vinyl_king",
                new AuctionStatusDetail.ScheduledDetail()
        );
    }

    @Test
    @DisplayName("경매 상세 조회 요청을 서비스에 위임하고 결과를 감싸 반환한다")
    void testGetAuctionDetail_delegatesToServiceAndWrapsResult() {
        // given
        given(auctionService.getAuctionDetail(1L, 2L)).willReturn(detailResult());

        // when
        ApiResponse<AuctionDetailResponse> response = auctionController.getAuctionDetail(1L, 2L);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().auctionId()).isEqualTo(1L);
        assertThat(response.getData().status()).isEqualTo("SCHEDULED");
        assertThat(response.getData().product().title()).isEqualTo("Abbey Road");
        assertThat(response.getData().seller().nickname()).isEqualTo("vinyl_king");
    }

    @Test
    @DisplayName("X-Member-Id 헤더 없이도 경매 상세를 조회할 수 있다")
    void testGetAuctionDetail_withoutViewer_succeeds() {
        // given
        given(auctionService.getAuctionDetail(1L, null)).willReturn(detailResult());

        // when
        ApiResponse<AuctionDetailResponse> response = auctionController.getAuctionDetail(1L, null);

        // then
        assertThat(response.isSuccess()).isTrue();
        verify(auctionService).getAuctionDetail(1L, null);
    }
}
