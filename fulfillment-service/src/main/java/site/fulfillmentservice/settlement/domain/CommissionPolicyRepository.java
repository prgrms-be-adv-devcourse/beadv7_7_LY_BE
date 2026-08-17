package site.fulfillmentservice.settlement.domain;

import java.util.Optional;

public interface CommissionPolicyRepository {

    CommissionPolicy save(CommissionPolicy commissionPolicy);

    CommissionPolicy saveAndFlush(CommissionPolicy commissionPolicy);

    Optional<CommissionPolicy> findById(Long id);

    Optional<CommissionPolicy> findByEffectiveToIsNull();
}
