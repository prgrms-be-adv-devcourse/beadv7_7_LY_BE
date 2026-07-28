package site.coreservice.auction.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionSearchViewTest {

    private final Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));
    private final AuctionSchedule schedule = AuctionSchedule.of(
            Period.of(LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0)),
            false, null
    );
    private final ProductSnapshot productSnapshot =
            new ProductSnapshot(100L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", true);
    private final LocalDateTime registerNow = schedule.getPeriod().getStartAt().minusHours(1);

    @Test
    @DisplayName("경매, 상품, 판매자 정보를 하나의 뷰로 합친다")
    void testFrom_mapsAuctionAndProductFields() {
        // given
        ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", List.of("1.png", "2.png"));
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule, registerNow);

        // when
        AuctionSearchView view = AuctionSearchView.from(auction, productSnapshot, "vinyl_king");

        // then
        assertThat(view.getProductId()).isEqualTo(100L);
        assertThat(view.getTitle()).isEqualTo("Abbey Road");
        assertThat(view.getArtistName()).isEqualTo("The Beatles");
        assertThat(view.getItemCondition()).isEqualTo(ItemCondition.MINT);
        assertThat(view.getThumbnail()).isEqualTo("1.png");
        assertThat(view.getSellerId()).isEqualTo(1L);
        assertThat(view.getSellerNickname()).isEqualTo("vinyl_king");
        assertThat(view.getHighestBidAmount()).isEqualTo(BigDecimal.valueOf(13_000L));
        assertThat(view.getBidCount()).isZero();
    }

    @Test
    @DisplayName("이미지가 없으면 썸네일은 null이다")
    void testFrom_nullImages_thumbnailIsNull() {
        // given
        ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", null);
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule, registerNow);

        // when
        AuctionSearchView view = AuctionSearchView.from(auction, productSnapshot, "vinyl_king");

        // then
        assertThat(view.getThumbnail()).isNull();
    }

    @Test
    @DisplayName("이미지 목록이 비어있으면 썸네일은 null이다")
    void testFrom_emptyImages_thumbnailIsNull() {
        // given
        ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", List.of());
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule, registerNow);

        // when
        AuctionSearchView view = AuctionSearchView.from(auction, productSnapshot, "vinyl_king");

        // then
        assertThat(view.getThumbnail()).isNull();
    }
}
