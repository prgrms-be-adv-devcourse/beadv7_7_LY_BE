package site.coreservice.auction.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.response.ApiResponse;
import site.coreservice.auction.application.InternalAuctionService;
import site.coreservice.auction.application.dto.InternalAuctionCountResult;
import site.coreservice.auction.application.dto.InternalAuctionSummaryResult;
import site.coreservice.auction.presentation.dto.InternalAuctionCountResponse;
import site.coreservice.auction.presentation.dto.InternalAuctionSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InternalAuctionControllerTest {

    @Mock
    private InternalAuctionService internalAuctionService;

    @InjectMocks
    private InternalAuctionController internalAuctionController;

    @Test
    @DisplayName("경매 내부 요약 조회 요청을 서비스에 위임하고 결과를 감싸 반환한다")
    void testGetSummary_delegatesToServiceAndWrapsResult() {
        // given
        InternalAuctionSummaryResult result = new InternalAuctionSummaryResult(
                1L, 100L, "MINT", 3, BigDecimal.valueOf(15_000),
                LocalDateTime.of(2026, 7, 2, 0, 0), "ENDED_WON"
        );
        given(internalAuctionService.getInternalSummary(1L)).willReturn(result);

        // when
        ApiResponse<InternalAuctionSummaryResponse> response = internalAuctionController.getAuction(1L);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().auctionId()).isEqualTo(1L);
        assertThat(response.getData().productId()).isEqualTo(100L);
        assertThat(response.getData().bidCount()).isEqualTo(3);
        assertThat(response.getData().finalPrice()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
        assertThat(response.getData().status()).isEqualTo("ENDED_WON");
    }

    @Test
    @DisplayName("진행 중인 경매 수 벌크 조회 요청을 서비스에 위임하고 counts로 감싸 반환한다")
    void testGetAuctionCounts_delegatesToServiceAndWrapsInCounts() {
        // given
        List<InternalAuctionCountResult> results = List.of(
                new InternalAuctionCountResult(55L, 3L),
                new InternalAuctionCountResult(56L, 0L)
        );
        given(internalAuctionService.getOpenAuctionCounts(List.of(55L, 56L))).willReturn(results);

        // when
        ApiResponse<InternalAuctionCountResponse> response = internalAuctionController.getAuctionCounts(List.of(55L, 56L));

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().counts()).hasSize(2);
        assertThat(response.getData().counts().get(0).productId()).isEqualTo(55L);
        assertThat(response.getData().counts().get(0).openAuctionCount()).isEqualTo(3L);
        assertThat(response.getData().counts().get(1).productId()).isEqualTo(56L);
        assertThat(response.getData().counts().get(1).openAuctionCount()).isZero();
    }

    @Test
    @DisplayName("productIds가 비어있으면 counts도 빈 배열을 반환한다")
    void testGetAuctionCounts_emptyProductIds_returnsEmptyCounts() {
        // given
        given(internalAuctionService.getOpenAuctionCounts(List.of())).willReturn(List.of());

        // when
        ApiResponse<InternalAuctionCountResponse> response = internalAuctionController.getAuctionCounts(List.of());

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().counts()).isEmpty();
    }
}
