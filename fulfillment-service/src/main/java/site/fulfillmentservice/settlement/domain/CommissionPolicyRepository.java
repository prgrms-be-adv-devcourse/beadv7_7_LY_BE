package site.fulfillmentservice.settlement.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CommissionPolicyRepository {

    CommissionPolicy save(CommissionPolicy commissionPolicy);

    CommissionPolicy saveAndFlush(CommissionPolicy commissionPolicy);

    Optional<CommissionPolicy> findById(Long id);

    Optional<CommissionPolicy> findByEffectiveToIsNull();

    Optional<CommissionPolicy> findByEffectiveTo(LocalDateTime effectiveTo);

    void deleteAndFlush(CommissionPolicy commissionPolicy);
}
