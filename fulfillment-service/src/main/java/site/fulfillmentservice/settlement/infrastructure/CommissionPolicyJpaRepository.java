package site.fulfillmentservice.settlement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;

public interface CommissionPolicyJpaRepository extends JpaRepository<CommissionPolicy, Long> {

    Optional<CommissionPolicy> findByEffectiveToIsNull();

    Optional<CommissionPolicy> findByEffectiveTo(LocalDateTime effectiveTo);

    List<CommissionPolicy> findAllByOrderByEffectiveFromDesc();

    @Query("SELECT c FROM CommissionPolicy c "
            + "WHERE c.effectiveFrom <= :dateTime AND (c.effectiveTo IS NULL OR c.effectiveTo > :dateTime)")
    Optional<CommissionPolicy> findEffectiveAt(@Param("dateTime") LocalDateTime dateTime);
}
