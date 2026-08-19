package site.fulfillmentservice.settlement.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommissionPolicyRepository {

    CommissionPolicy save(CommissionPolicy commissionPolicy);

    CommissionPolicy saveAndFlush(CommissionPolicy commissionPolicy);

    Optional<CommissionPolicy> findById(Long id);

    Optional<CommissionPolicy> findByEffectiveToIsNull();

    Optional<CommissionPolicy> findByEffectiveTo(LocalDateTime effectiveTo);

    List<CommissionPolicy> findAllByOrderByEffectiveFromDesc();

    Optional<CommissionPolicy> findEffectiveAt(LocalDateTime dateTime);

    void deleteAndFlush(CommissionPolicy commissionPolicy);
}
