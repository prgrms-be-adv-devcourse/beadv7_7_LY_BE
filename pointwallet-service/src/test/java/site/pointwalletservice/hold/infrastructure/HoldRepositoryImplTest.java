package site.pointwalletservice.hold.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import site.pointwalletservice.hold.domain.Hold;
import site.pointwalletservice.hold.domain.HoldRepository;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.support.RepositoryTest;


@RepositoryTest
@Import(HoldRepositoryImpl.class)
class HoldRepositoryImplTest {

    @Autowired
    private HoldRepository holdRepository;

    @Autowired
    private HoldJpaRepository holdJpaRepository;

    @Test
    void findByAuctionId는_해당_경매의_활성_홀드를_반환한다() {
        holdJpaRepository.save(Hold.place(5001L, 456L, Money.of(15_000)));

        Optional<Hold> result = holdRepository.findByAuctionId(5001L);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(456L);
    }

    @Test
    void findByAuctionId_활성_홀드_없으면_빈값() {
        Optional<Hold> result = holdRepository.findByAuctionId(9999L);

        assertThat(result).isEmpty();
    }

    @Test
    void 같은_경매에_두번째_홀드를_저장하면_유니크_제약으로_실패한다() {
        holdJpaRepository.saveAndFlush(Hold.place(5001L, 456L, Money.of(15_000)));

        assertThatThrownBy(() ->
                holdJpaRepository.saveAndFlush(Hold.place(5001L, 789L, Money.of(20_000)))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void delete하면_레코드가_사라져서_활성_홀드가_아니게_된다() {
        Hold hold = holdJpaRepository.save(Hold.place(5001L, 456L, Money.of(15_000)));

        holdRepository.delete(hold);

        assertThat(holdRepository.findByAuctionId(5001L)).isEmpty();
    }
}