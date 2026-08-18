package site.fulfillmentservice.settlement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;

public interface CommissionPolicyJpaRepository extends JpaRepository<CommissionPolicy, Long> {

    Optional<CommissionPolicy> findByEffectiveToIsNull();

    Optional<CommissionPolicy> findByEffectiveTo(LocalDateTime effectiveTo);

    List<CommissionPolicy> findAllByOrderByEffectiveFromDesc();
}
