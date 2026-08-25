package site.pointwalletservice.wallet.deadletter.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.pointwalletservice.support.RepositoryTest;
import site.pointwalletservice.wallet.deadletter.domain.DeadLetterStatus;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetter;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetterRepository;

@RepositoryTest
@Import(WithdrawFeeDeadLetterRepositoryImpl.class)
class WithdrawFeeDeadLetterRepositoryImplTest {

    @Autowired
    private WithdrawFeeDeadLetterRepository repository;

    @Autowired
    private WithdrawFeeDeadLetterJpaRepository jpaRepository;

    @Test
    void save와_findById로_저장한_레코드를_그대로_조회한다() {
        WithdrawFeeDeadLetter saved = repository.save(
                WithdrawFeeDeadLetter.open(1L, BigDecimal.valueOf(2_000), "커넥션 타임아웃"));

        Optional<WithdrawFeeDeadLetter> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getWithdrawId()).isEqualTo(1L);
        assertThat(result.get().getFeeAmount()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
    }

    @Test
    void findById_존재하지_않으면_빈값() {
        Optional<WithdrawFeeDeadLetter> result = repository.findById(9999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusOrderByCreatedAtDesc는_해당_상태만_최신순으로_반환한다() {
        WithdrawFeeDeadLetter open1 = jpaRepository.save(
                WithdrawFeeDeadLetter.open(1L, BigDecimal.valueOf(1_000), "실패1"));
        WithdrawFeeDeadLetter open2 = jpaRepository.save(
                WithdrawFeeDeadLetter.open(2L, BigDecimal.valueOf(2_000), "실패2"));
        WithdrawFeeDeadLetter resolved = WithdrawFeeDeadLetter.open(3L, BigDecimal.valueOf(3_000), "실패3");
        resolved.resolve("처리 완료");
        jpaRepository.save(resolved);

        List<WithdrawFeeDeadLetter> result =
                repository.findByStatusOrderByCreatedAtDesc(DeadLetterStatus.OPEN);

        assertThat(result).extracting(WithdrawFeeDeadLetter::getWithdrawId)
                .containsExactlyInAnyOrder(open1.getWithdrawId(), open2.getWithdrawId())
                .doesNotContain(resolved.getWithdrawId());
    }
}