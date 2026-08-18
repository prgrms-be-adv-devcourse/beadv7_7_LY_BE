package site.fulfillmentservice.settlement.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.fulfillmentservice.settlement.application.dto.CommissionPolicyResult;
import site.fulfillmentservice.settlement.application.dto.CreateCommissionPolicyCommand;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;
import site.fulfillmentservice.settlement.domain.CommissionPolicyRepository;
import site.fulfillmentservice.settlement.exception.SettlementErrorCode;
import site.fulfillmentservice.settlement.exception.SettlementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommissionPolicyService {

    private final CommissionPolicyRepository commissionPolicyRepository;

    public CommissionPolicyResult createCommissionPolicy(CreateCommissionPolicyCommand command, Long adminId) {
        validateEffectiveFromDate(command.effectiveFromDate());
        LocalDateTime effectiveFrom = command.effectiveFromDate().atStartOfDay();

        Optional<CommissionPolicy> openPolicy = commissionPolicyRepository.findByEffectiveToIsNull();
        if (openPolicy.isPresent() && openPolicy.get().isPending(LocalDateTime.now())) {
            throw new SettlementException(SettlementErrorCode.PENDING_COMMISSION_POLICY_ALREADY_EXISTS);
        }

        openPolicy.ifPresent(predecessor -> closePredecessor(predecessor, effectiveFrom));

        CommissionPolicy newPolicy = commissionPolicyRepository.save(
                CommissionPolicy.of(command.commissionRate(), effectiveFrom, null));

        log.info("수수료 정책 등록: adminId={}, commissionPolicyId={}, commissionRate={}, effectiveFrom={}",
                adminId, newPolicy.getId(), newPolicy.getCommissionRate(), newPolicy.getEffectiveFrom());

        return CommissionPolicyResult.from(newPolicy);
    }

    public void deleteCommissionPolicy(Long id, Long adminId) {
        CommissionPolicy target = commissionPolicyRepository.findById(id)
                .orElseThrow(() -> new SettlementException(SettlementErrorCode.COMMISSION_POLICY_NOT_FOUND));

        if (!target.isPending(LocalDateTime.now())) {
            throw new SettlementException(SettlementErrorCode.COMMISSION_POLICY_NOT_DELETABLE);
        }

        commissionPolicyRepository.findByEffectiveTo(target.getEffectiveFrom())
                .ifPresent(this::reopenPredecessor);

        try {
            commissionPolicyRepository.deleteAndFlush(target);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SettlementException(SettlementErrorCode.COMMISSION_POLICY_CONFLICT);
        }

        log.info("수수료 정책 삭제: adminId={}, commissionPolicyId={}, commissionRate={}, effectiveFrom={}",
                adminId, target.getId(), target.getCommissionRate(), target.getEffectiveFrom());
    }

    @Transactional(readOnly = true)
    public List<CommissionPolicyResult> getCommissionPolicies() {
        return commissionPolicyRepository.findAllByOrderByEffectiveFromDesc().stream()
                .map(CommissionPolicyResult::from)
                .toList();
    }

    private void reopenPredecessor(CommissionPolicy predecessor) {
        predecessor.reopen();
        try {
            commissionPolicyRepository.saveAndFlush(predecessor);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SettlementException(SettlementErrorCode.COMMISSION_POLICY_CONFLICT);
        }
    }

    private void closePredecessor(CommissionPolicy predecessor, LocalDateTime effectiveFrom) {
        predecessor.close(effectiveFrom);
        try {
            commissionPolicyRepository.saveAndFlush(predecessor);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SettlementException(SettlementErrorCode.COMMISSION_POLICY_CONFLICT);
        }
    }

    private void validateEffectiveFromDate(LocalDate effectiveFromDate) {
        if (effectiveFromDate == null || !effectiveFromDate.isAfter(LocalDate.now())) {
            throw new SettlementException(SettlementErrorCode.INVALID_COMMISSION_POLICY_EFFECTIVE_FROM_DATE);
        }
    }
}
