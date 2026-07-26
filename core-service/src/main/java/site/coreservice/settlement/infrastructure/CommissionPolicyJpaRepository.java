package site.coreservice.settlement.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.settlement.domain.CommissionPolicy;

public interface CommissionPolicyJpaRepository extends JpaRepository<CommissionPolicy, Long> {

    Optional<CommissionPolicy> findByEffectiveToIsNull();
}
