package site.coreservice.pointwallet.withdraw.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.withdraw.domain.Withdraw;
import site.coreservice.pointwallet.withdraw.domain.WithdrawRepository;
import site.coreservice.pointwallet.withdraw.domain.WithdrawStatus;
import site.coreservice.support.RepositoryTest;

@RepositoryTest
@Import(WithdrawRepositoryImpl.class)
class WithdrawRepositoryImplTest {

    @Autowired
    private WithdrawRepository withdrawRepository;

    @Test
    @DisplayName("저장하고 id로 조회하면 저장한 값 그대로(3개 Money 필드 포함) 돌아온다")
    void save_findById_정상동작() {
        // given
        Withdraw withdraw = Withdraw.request(1L, Money.of(100_000), Money.of(2_000), Money.of(98_000));

        // when
        Withdraw saved = withdrawRepository.save(withdraw);
        Optional<Withdraw> found = withdrawRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(Money.of(100_000));
        assertThat(found.get().getFeeAmount()).isEqualTo(Money.of(2_000));
        assertThat(found.get().getNetAmount()).isEqualTo(Money.of(98_000));
        assertThat(found.get().getStatus()).isEqualTo(WithdrawStatus.PENDING);
    }

    @Test
    @DisplayName("존재하지 않는 id로 조회하면 빈 Optional을 반환한다")
    void findById_없으면_empty() {
        // when
        Optional<Withdraw> found = withdrawRepository.findById(999_999L);

        // then
        assertThat(found).isEmpty();
    }
}