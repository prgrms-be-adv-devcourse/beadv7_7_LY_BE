package site.coreservice.auction.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;
import site.coreservice.support.RepositoryTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@Import(AuctionRepositoryImpl.class)
class AuctionRepositoryImplTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionJpaRepository auctionJpaRepository;

    @Test
    @DisplayName("경매를 저장하면 모든 값 객체를 포함하여 조회된다")
    void testSave_reloadsAuctionWithAllVoValues() {
        final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "실제 저장 확인용 상품 설명입니다.", List.of("1.png", "2.png"));
        final Pricing pricing = Pricing.of(Money.from(10_000L), Money.from(500L), Money.from(3_000L));
        final AuctionSchedule schedule = AuctionSchedule.of(
                Period.of(LocalDateTime.now(), LocalDateTime.now().plusDays(1)),
                true, 5
        );
        final Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);

        final Auction saved = auctionRepository.save(auction);
        final Auction found = auctionJpaRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getSellerId()).isEqualTo(1L);
        assertThat(found.getProductId()).isEqualTo(100L);
        assertThat(found.getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
        assertThat(found.getItemInfo().getImageUrls()).containsExactly("1.png", "2.png");
        assertThat(found.getPricing().getStartPrice()).isEqualTo(Money.from(10_000L));
        assertThat(found.getSchedule().getExtensionTime()).isEqualTo(5);
        assertThat(found.hasBid()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 경매 ID로 조회하면 빈 Optional을 반환한다")
    void testFindById_notFound_returnsEmpty() {
        final Optional<Auction> result = auctionRepository.findById(999L);

        assertThat(result).isEmpty();
    }
}
