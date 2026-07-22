package site.coreservice.auction.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.coreservice.auction.domain.Bid;
import site.coreservice.auction.domain.BidOutcome;
import site.coreservice.auction.domain.BidRepository;
import site.coreservice.auction.domain.Money;
import site.coreservice.support.RepositoryTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@Import(BidRepositoryImpl.class)
class BidRepositoryImplTest {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BidJpaRepository bidJpaRepository;

    @Test
    void testSave_reloadsBidWithAllValues() {
        final Bid bid = Bid.place(1L, 2L, Money.of(10_500L), LocalDateTime.now());

        final Bid saved = bidRepository.save(bid);
        final Bid found = bidJpaRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getAuctionId()).isEqualTo(1L);
        assertThat(found.getBidderId()).isEqualTo(2L);
        assertThat(found.getAmount()).isEqualTo(Money.of(10_500L));
        assertThat(found.getOutcome()).isEqualTo(BidOutcome.ACTIVE);
    }

    @Test
    void testFindById_notFound_returnsEmpty() {
        final Optional<Bid> result = bidRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void testFindActiveBid_returnsOnlyActiveBid() {
        final Bid activeBid = Bid.place(1L, 2L, Money.of(10_500L), LocalDateTime.now());
        bidJpaRepository.save(activeBid);

        final Bid outbidBid = Bid.place(1L, 3L, Money.of(9_500L), LocalDateTime.now());
        outbidBid.markOutbid();
        bidJpaRepository.save(outbidBid);

        final Optional<Bid> result = bidRepository.findActiveBid(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getBidderId()).isEqualTo(2L);
    }

    @Test
    void testFindActiveBid_noActiveBid_returnsEmpty() {
        final Bid outbidBid = Bid.place(1L, 3L, Money.of(9_500L), LocalDateTime.now());
        outbidBid.markOutbid();
        bidJpaRepository.save(outbidBid);

        final Optional<Bid> result = bidRepository.findActiveBid(1L);

        assertThat(result).isEmpty();
    }
}
