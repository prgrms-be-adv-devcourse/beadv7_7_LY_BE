package site.coreservice.settlement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.coreservice.settlement.domain.CommissionPolicy;
import site.coreservice.settlement.domain.CommissionPolicyRepository;
import site.coreservice.support.RepositoryTest;

@RepositoryTest
@Import(CommissionPolicyRepositoryImpl.class)
class CommissionPolicyRepositoryImplTest {

    @Autowired
    private CommissionPolicyRepository commissionPolicyRepository;

    @Autowired
    private CommissionPolicyJpaRepository commissionPolicyJpaRepository;

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
}
