package site.pointwalletservice.withdraw.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.support.RepositoryTest;
import site.pointwalletservice.withdraw.domain.Withdraw;
import site.pointwalletservice.withdraw.domain.WithdrawRepository;
import site.pointwalletservice.withdraw.domain.WithdrawStatus;

@RepositoryTest
@Import(WithdrawRepositoryImpl.class)
class WithdrawRepositoryImplTest {

    @Autowired
    private WithdrawRepository withdrawRepository;

    @Test
    @DisplayName("저장하고 id로 조회하면 저장한 값 그대로(3개 Money 필드 + 멱등키 포함) 돌아온다")
    void save_findById_정상동작() {
        // given
        Withdraw withdraw = Withdraw.request(1L, Money.of(100_000), Money.of(2_000), Money.of(98_000), "idem-key-1");

        // when
        Withdraw saved = withdrawRepository.save(withdraw);
        Optional<Withdraw> found = withdrawRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(Money.of(100_000));
        assertThat(found.get().getFeeAmount()).isEqualTo(Money.of(2_000));
        assertThat(found.get().getNetAmount()).isEqualTo(Money.of(98_000));
        assertThat(found.get().getIdempotencyKey()).isEqualTo("idem-key-1");
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

    @Test
    @DisplayName("멱등키로 조회하면 저장한 건을 찾는다")
    void findByIdempotencyKey_정상조회() {
        // given
        withdrawRepository.save(
                Withdraw.request(1L, Money.of(100_000), Money.of(2_000), Money.of(98_000), "idem-key-2"));

        // when
        Optional<Withdraw> found = withdrawRepository.findByIdempotencyKey("idem-key-2");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 멱등키로 조회하면 빈 Optional을 반환한다")
    void findByIdempotencyKey_없으면_empty() {
        // when
        Optional<Withdraw> found = withdrawRepository.findByIdempotencyKey("no-such-key");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("같은 멱등키로 두 번 저장하면 유니크 제약 위반이 난다")
    void 같은_멱등키_중복저장시_예외() {
        // given
        withdrawRepository.save(
                Withdraw.request(1L, Money.of(100_000), Money.of(2_000), Money.of(98_000), "idem-key-3"));

        // when & then
        assertThatThrownBy(() -> withdrawRepository.save(
                Withdraw.request(2L, Money.of(50_000), Money.of(1_000), Money.of(49_000), "idem-key-3")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}