package site.coreservice.auction.infrastructure;

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
    void testSave_reloadsAuctionWithAllVoValues() {
        final ItemInfo itemInfo = ItemInfo.from(ItemCondition.MINT, "실제 저장 확인용 상품 설명입니다.", List.of("1.png", "2.png"));
        final Pricing pricing = Pricing.from(Money.of(10_000L), Money.of(500L), Money.of(3_000L));
        final AuctionSchedule schedule = AuctionSchedule.from(
                Period.from(LocalDateTime.now(), LocalDateTime.now().plusDays(1)),
                true, 5
        );
        final Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule);

        final Auction saved = auctionRepository.save(auction);
        final Auction found = auctionJpaRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getSellerId()).isEqualTo(1L);
        assertThat(found.getProductId()).isEqualTo(100L);
        assertThat(found.getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
        assertThat(found.getItemInfo().getImageUrls()).containsExactly("1.png", "2.png");
        assertThat(found.getPricing().getStartPrice()).isEqualTo(Money.of(10_000L));
        assertThat(found.getSchedule().getExtensionTime()).isEqualTo(5);
        assertThat(found.hasBid()).isFalse();
    }

    @Test
    void testFindById_notFound_returnsEmpty() {
        final Optional<Auction> result = auctionRepository.findById(999L);

        assertThat(result).isEmpty();
    }
}
