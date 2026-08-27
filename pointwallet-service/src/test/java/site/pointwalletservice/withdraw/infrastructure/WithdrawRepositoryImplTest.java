package site.pointwalletservice.withdraw.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
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

    private static final String IDEMPOTENCY_KEY = "test-idem-key-0001";

    @Test
    @DisplayName("저장하고 id로 조회하면 저장한 값 그대로(3개 Money 필드 + 멱등키) 돌아온다")
    void save_findById_정상동작() {
        // given
        Withdraw withdraw = Withdraw.request(1L, IDEMPOTENCY_KEY, Money.of(100_000), Money.of(2_000), Money.of(98_000));

        // when
        Withdraw saved = withdrawRepository.save(withdraw);
        Optional<Withdraw> found = withdrawRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(Money.of(100_000));
        assertThat(found.get().getFeeAmount()).isEqualTo(Money.of(2_000));
        assertThat(found.get().getNetAmount()).isEqualTo(Money.of(98_000));
        assertThat(found.get().getStatus()).isEqualTo(WithdrawStatus.PENDING);
        assertThat(found.get().getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
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
    @DisplayName("findByUserIdAndIdempotencyKey()는 (userId, idempotencyKey)가 모두 일치해야 찾는다 — " +
            "같은 키라도 다른 userId로 조회하면 빈 Optional이어야 한다 (BOLA 방지의 핵심 전제)")
    void findByUserIdAndIdempotencyKey_userId까지_일치해야_조회된다() {
        // given: userId=1이 idempotencyKey를 사용해 인출을 저장함
        Long ownerId = 1L;
        Long strangerId = 2L;
        withdrawRepository.save(
                Withdraw.request(ownerId, IDEMPOTENCY_KEY, Money.of(100_000), Money.of(2_000), Money.of(98_000)));

        // when
        Optional<Withdraw> byOwner = withdrawRepository.findByUserIdAndIdempotencyKey(ownerId, IDEMPOTENCY_KEY);
        Optional<Withdraw> byStranger = withdrawRepository.findByUserIdAndIdempotencyKey(strangerId, IDEMPOTENCY_KEY);

        // then — 소유자로 조회하면 찾고, 같은 키를 다른 userId로 조회하면 절대 찾으면 안 된다
        assertThat(byOwner).isPresent();
        assertThat(byOwner.get().getUserId()).isEqualTo(ownerId);
        assertThat(byStranger).isEmpty();
    }
}