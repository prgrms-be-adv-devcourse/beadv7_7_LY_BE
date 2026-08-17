package site.fulfillmentservice.settlement.infrastructure;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;
import site.fulfillmentservice.settlement.domain.CommissionPolicyRepository;

@Repository
@RequiredArgsConstructor
public class CommissionPolicyRepositoryImpl implements CommissionPolicyRepository {

    private final CommissionPolicyJpaRepository commissionPolicyJpaRepository;

    @Override
    public CommissionPolicy save(CommissionPolicy commissionPolicy) {
        return commissionPolicyJpaRepository.save(commissionPolicy);
    }

    @Override
    public CommissionPolicy saveAndFlush(CommissionPolicy commissionPolicy) {
        return commissionPolicyJpaRepository.saveAndFlush(commissionPolicy);
    }

    @Override
    public Optional<CommissionPolicy> findById(Long id) {
        return commissionPolicyJpaRepository.findById(id);
    }

    @Override
    public Optional<CommissionPolicy> findByEffectiveToIsNull() {
        return commissionPolicyJpaRepository.findByEffectiveToIsNull();
    }
}