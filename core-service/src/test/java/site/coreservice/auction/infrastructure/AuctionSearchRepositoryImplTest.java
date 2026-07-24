package site.coreservice.auction.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;
import site.coreservice.auction.exception.AuctionSearchViewNotFoundException;
import site.coreservice.support.RepositoryTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
@Import(AuctionSearchRepositoryImpl.class)
class AuctionSearchRepositoryImplTest {

    @Autowired
    private AuctionSearchViewRepository auctionSearchViewRepository;

    @Autowired
    private AuctionSearchViewJpaRepository searchViewJpaRepository;

    @Test
    @DisplayName("경매를 서치 뷰로 저장하면 상품·판매자 정보와 함께 조회된다")
    void testSave_persistsSearchView() {
        // given
        ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", List.of("1.png"));
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));
        AuctionSchedule schedule = AuctionSchedule.of(
                Period.of(LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0)),
                false, null
        );
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);
        ReflectionTestUtils.setField(auction, "id", 1L);
        ProductSnapshot productSnapshot = new ProductSnapshot(100L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", true);

        // when
        auctionSearchViewRepository.save(auction, productSnapshot, "vinyl_king");

        // then
        AuctionSearchView found = searchViewJpaRepository.findById(1L).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Abbey Road");
        assertThat(found.getSellerNickname()).isEqualTo("vinyl_king");
        assertThat(found.getThumbnail()).isEqualTo("1.png");
    }

    @Test
    @DisplayName("경매 상태가 바뀌면 서치 뷰의 상태도 갱신된다")
    void testUpdateStatus_updatesPersistedStatus() {
        // given
        ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", List.of("1.png"));
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));
        AuctionSchedule schedule = AuctionSchedule.of(
                Period.of(LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0)),
                false, null
        );
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);
        ReflectionTestUtils.setField(auction, "id", 1L);
        ProductSnapshot productSnapshot = new ProductSnapshot(100L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", true);
        auctionSearchViewRepository.save(auction, productSnapshot, "vinyl_king");
        auction.changeStatus(AuctionStatus.RUNNING);

        // when
        auctionSearchViewRepository.updateStatus(auction);

        // then
        AuctionSearchView found = searchViewJpaRepository.findById(1L).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    @DisplayName("존재하지 않는 서치 뷰의 상태를 갱신하려 하면 예외를 던진다")
    void testUpdateStatus_viewNotFound_throws() {
        // given
        ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", List.of("1.png"));
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));
        AuctionSchedule schedule = AuctionSchedule.of(
                Period.of(LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0)),
                false, null
        );
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);
        ReflectionTestUtils.setField(auction, "id", 999L);
        auction.changeStatus(AuctionStatus.RUNNING);

        // when & then
        assertThatThrownBy(() -> auctionSearchViewRepository.updateStatus(auction))
                .isInstanceOf(AuctionSearchViewNotFoundException.class);
    }
}
