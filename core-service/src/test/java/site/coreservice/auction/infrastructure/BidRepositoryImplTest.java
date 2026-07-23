package site.coreservice.auction.infrastructure;

import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("입찰을 저장하면 모든 값이 정상적으로 조회된다")
    void testSave_reloadsBidWithAllValues() {
        final Bid bid = Bid.place(1L, 2L, Money.from(10_500L), LocalDateTime.now());

        final Bid saved = bidRepository.save(bid);
        final Bid found = bidJpaRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getAuctionId()).isEqualTo(1L);
        assertThat(found.getBidderId()).isEqualTo(2L);
        assertThat(found.getAmount()).isEqualTo(Money.from(10_500L));
        assertThat(found.getOutcome()).isEqualTo(BidOutcome.ACTIVE);
    }

    @Test
    @DisplayName("존재하지 않는 입찰 ID로 조회하면 빈 Optional을 반환한다")
    void testFindById_notFound_returnsEmpty() {
        final Optional<Bid> result = bidRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("진행 중인 입찰만 조회한다")
    void testFindActiveBid_returnsOnlyActiveBid() {
        final Bid activeBid = Bid.place(1L, 2L, Money.from(10_500L), LocalDateTime.now());
        bidJpaRepository.save(activeBid);

        final Bid outbidBid = Bid.place(1L, 3L, Money.from(9_500L), LocalDateTime.now());
        outbidBid.markOutbid();
        bidJpaRepository.save(outbidBid);

        final Optional<Bid> result = bidRepository.findActiveBid(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getBidderId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("진행 중인 입찰이 없으면 빈 Optional을 반환한다")
    void testFindActiveBid_noActiveBid_returnsEmpty() {
        final Bid outbidBid = Bid.place(1L, 3L, Money.from(9_500L), LocalDateTime.now());
        outbidBid.markOutbid();
        bidJpaRepository.save(outbidBid);

        final Optional<Bid> result = bidRepository.findActiveBid(1L);

        assertThat(result).isEmpty();
    }
}
