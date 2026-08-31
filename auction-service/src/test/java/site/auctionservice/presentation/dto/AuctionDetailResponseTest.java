package site.auctionservice.presentation.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.auctionservice.application.dto.AuctionDetailResult;
import site.auctionservice.application.dto.AuctionStatusDetail;
import site.auctionservice.application.dto.BidDetailResult;
import site.auctionservice.application.port.dto.ProductDetail;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionDetailResponseTest {

    private static final LocalDateTime START_AT = LocalDateTime.of(2026, 7, 1, 0, 0);
    private static final LocalDateTime END_AT = LocalDateTime.of(2026, 7, 2, 0, 0);

    private final ProductDetail product = new ProductDetail(100L, "Abbey Road", "The Beatles",
            "https://cdn.example.com/cover.jpg", 1969, "Rock", "ORIGINAL", true);

    private AuctionDetailResult resultWith(AuctionStatusDetail detail) {
        return new AuctionDetailResult(
                1L,
                "MINT", "충분히 긴 상품 설명입니다.", List.of("1.png"),
                BigDecimal.valueOf(10_000), BigDecimal.valueOf(3_000),
                BigDecimal.valueOf(13_000), BigDecimal.valueOf(500),
                START_AT, END_AT,
                false, null,
                product, 1L, "vinyl_king",
                detail
        );
    }

    @Test
    @DisplayName("ScheduledDetail이면 status만 SCHEDULED로 채워지고 나머지 상태별 필드는 비어있다")
    void testFrom_scheduledDetail_fillsStatusOnly() {
        // when
        AuctionDetailResponse response = AuctionDetailResponse.from(resultWith(new AuctionStatusDetail.ScheduledDetail()));

        // then
        assertThat(response.status()).isEqualTo("SCHEDULED");
        assertThat(response.highestBidAmount()).isNull();
        assertThat(response.nextMinBidAmount()).isNull();
        assertThat(response.bidCount()).isNull();
        assertThat(response.recentBids()).isNull();
        assertThat(response.myHighest()).isNull();
        assertThat(response.won()).isNull();
        assertThat(response.winningBid()).isNull();
    }

    @Test
    @DisplayName("RunningDetail이면 status는 RUNNING이고 입찰 관련 필드가 채워진다")
    void testFrom_runningDetail_fillsBidFields() {
        // given
        BidDetailResult bid = new BidDetailResult("bidder_2", BigDecimal.valueOf(12_000), START_AT.plusMinutes(1));
        AuctionStatusDetail.RunningDetail running = new AuctionStatusDetail.RunningDetail(
                BigDecimal.valueOf(12_000), BigDecimal.valueOf(12_500), 1L, List.of(bid), true
        );

        // when
        AuctionDetailResponse response = AuctionDetailResponse.from(resultWith(running));

        // then
        assertThat(response.status()).isEqualTo("RUNNING");
        assertThat(response.highestBidAmount()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(response.nextMinBidAmount()).isEqualByComparingTo(BigDecimal.valueOf(12_500));
        assertThat(response.bidCount()).isEqualTo(1L);
        assertThat(response.myHighest()).isTrue();
        assertThat(response.recentBids()).hasSize(1);
        assertThat(response.recentBids().getFirst().bidder()).isEqualTo("bidder_2");
        assertThat(response.won()).isNull();
        assertThat(response.winningBid()).isNull();
    }

    @Test
    @DisplayName("ClosingDetail이면 status는 CLOSING이고 낙찰 여부/입찰 정보는 노출되지 않는다")
    void testFrom_closingDetail_hidesOutcome() {
        // when
        AuctionDetailResponse response = AuctionDetailResponse.from(resultWith(new AuctionStatusDetail.ClosingDetail()));

        // then
        assertThat(response.status()).isEqualTo("CLOSING");
        assertThat(response.won()).isNull();
        assertThat(response.winningBid()).isNull();
        assertThat(response.highestBidAmount()).isNull();
    }

    @Test
    @DisplayName("EndedWonDetail이면 status는 ENDED_WON이고 won=true, 낙찰 정보가 채워진다")
    void testFrom_endedWonDetail_fillsWinningBid() {
        // given
        BidDetailResult winningBid = new BidDetailResult("winner_3", BigDecimal.valueOf(15_000), START_AT.plusMinutes(1));

        // when
        AuctionDetailResponse response = AuctionDetailResponse.from(
                resultWith(new AuctionStatusDetail.EndedWonDetail(winningBid, List.of(winningBid))));

        // then
        assertThat(response.status()).isEqualTo("ENDED_WON");
        assertThat(response.won()).isTrue();
        assertThat(response.winningBid().bidder()).isEqualTo("winner_3");
        assertThat(response.winningBid().amount()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
        assertThat(response.recentBids()).hasSize(1);
        assertThat(response.recentBids().get(0).bidder()).isEqualTo("winner_3");
    }

    @Test
    @DisplayName("EndedFailedDetail이면 status는 ENDED_FAILED이고 won=false, 낙찰 정보는 없다")
    void testFrom_endedFailedDetail_wonFalseWithoutWinningBid() {
        // when
        AuctionDetailResponse response = AuctionDetailResponse.from(resultWith(new AuctionStatusDetail.EndedFailedDetail()));

        // then
        assertThat(response.status()).isEqualTo("ENDED_FAILED");
        assertThat(response.won()).isFalse();
        assertThat(response.winningBid()).isNull();
    }

    @Test
    @DisplayName("공통 필드가 비어있으면 build() 시점에 예외가 발생한다")
    void testBuilder_missingRequiredField_throws() {
        // when & then
        assertThatThrownBy(() -> AuctionDetailResponse.builder()
                .status("SCHEDULED")
                .itemCondition("MINT")
                .startBidAmount(BigDecimal.valueOf(13_000))
                .bidUnit(BigDecimal.valueOf(500))
                .startAt(START_AT)
                .endAt(END_AT)
                .extensionEnabled(false)
                .product(AuctionDetailResponse.ProductInfo.from(product))
                .seller(AuctionDetailResponse.SellerInfo.of(1L, "vinyl_king"))
                // auctionId를 의도적으로 채우지 않음
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("응답을 JSON으로 직렬화하면 값이 없는 상태별 필드는 아예 나타나지 않는다")
    void testJsonSerialization_excludesNullFields() {
        // given
        AuctionDetailResponse response = AuctionDetailResponse.from(resultWith(new AuctionStatusDetail.ScheduledDetail()));
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        // when
        String json = objectMapper.writeValueAsString(response);

        // then
        assertThat(json).contains("\"status\":\"SCHEDULED\"");
        assertThat(json).doesNotContain("highestBidAmount");
        assertThat(json).doesNotContain("winningBid");
        assertThat(json).doesNotContain("recentBids");
    }
}
