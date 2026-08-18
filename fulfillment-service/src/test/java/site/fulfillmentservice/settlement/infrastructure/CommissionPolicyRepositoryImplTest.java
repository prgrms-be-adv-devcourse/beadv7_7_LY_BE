package site.fulfillmentservice.settlement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;
import site.fulfillmentservice.settlement.domain.CommissionPolicyRepository;
import site.fulfillmentservice.support.RepositoryTest;

@RepositoryTest
@Import(CommissionPolicyRepositoryImpl.class)
class CommissionPolicyRepositoryImplTest {

    @Autowired
    private CommissionPolicyRepository commissionPolicyRepository;

    @Autowired
    private CommissionPolicyJpaRepository commissionPolicyJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByEffectiveToIsNull은_현재_유효한_정책을_반환한다() {
        commissionPolicyJpaRepository.save(CommissionPolicy.of(
                BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1)));
        commissionPolicyJpaRepository.save(CommissionPolicy.of(
                BigDecimal.valueOf(0.1000), LocalDateTime.now().minusDays(1), null));

        Optional<CommissionPolicy> result = commissionPolicyRepository.findByEffectiveToIsNull();

        assertThat(result).isPresent();
        assertThat(result.get().getCommissionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.1000));
    }

    @Test
    void findByEffectiveToIsNull_없으면_빈값() {
        commissionPolicyJpaRepository.save(CommissionPolicy.of(
                BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1)));

        Optional<CommissionPolicy> result = commissionPolicyRepository.findByEffectiveToIsNull();

        assertThat(result).isEmpty();
    }

    @Test
    void 낙관적_락_충돌시_예외를_던진다() {
        CommissionPolicy saved = commissionPolicyJpaRepository.saveAndFlush(
                CommissionPolicy.of(BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(1), null));
        entityManager.clear();

        CommissionPolicy first = commissionPolicyRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        CommissionPolicy second = commissionPolicyRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        first.close(LocalDateTime.now().plusDays(1));
        commissionPolicyRepository.saveAndFlush(first);
        entityManager.clear();

        second.close(LocalDateTime.now().plusDays(2));
        assertThatThrownBy(() -> commissionPolicyRepository.saveAndFlush(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void reopen_낙관적_락_충돌시_예외를_던진다() {
        CommissionPolicy saved = commissionPolicyJpaRepository.saveAndFlush(
                CommissionPolicy.of(BigDecimal.valueOf(0.0500),
                        LocalDateTime.now().minusDays(10), LocalDateTime.now().plusDays(1)));
        entityManager.clear();

        CommissionPolicy first = commissionPolicyRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        CommissionPolicy second = commissionPolicyRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        first.reopen();
        commissionPolicyRepository.saveAndFlush(first);
        entityManager.clear();

        second.reopen();
        assertThatThrownBy(() -> commissionPolicyRepository.saveAndFlush(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void findAllByOrderByEffectiveFromDesc은_effectiveFrom_내림차순으로_반환한다() {
        CommissionPolicy older = commissionPolicyJpaRepository.save(CommissionPolicy.of(
                BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1)));
        CommissionPolicy newer = commissionPolicyJpaRepository.save(CommissionPolicy.of(
                BigDecimal.valueOf(0.1000), LocalDateTime.now().minusDays(1), null));

        List<CommissionPolicy> result = commissionPolicyRepository.findAllByOrderByEffectiveFromDesc();

        assertThat(result).extracting(CommissionPolicy::getId)
                .containsExactly(newer.getId(), older.getId());
    }
}
