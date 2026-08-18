package site.fulfillmentservice.settlement.infrastructure.seed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;
import site.fulfillmentservice.settlement.domain.CommissionPolicyRepository;

@Slf4j
@Profile("local")
@ConditionalOnProperty(prefix = "settlement.commission-policy-seed", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class CommissionPolicySeedLoader implements CommandLineRunner {

    private static final BigDecimal INITIAL_COMMISSION_RATE = BigDecimal.valueOf(0.0500);
    private static final LocalDateTime INITIAL_EFFECTIVE_FROM = LocalDateTime.of(2020, 1, 1, 0, 0);

    private final CommissionPolicyRepository commissionPolicyRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (commissionPolicyRepository.findByEffectiveToIsNull().isPresent()) {
            return;
        }
        CommissionPolicy seed = commissionPolicyRepository.save(
                CommissionPolicy.of(INITIAL_COMMISSION_RATE, INITIAL_EFFECTIVE_FROM, null));
        log.info("[CommissionPolicySeedLoader] 초기 수수료 정책 적재 완료 — commissionRate={}, effectiveFrom={}",
                seed.getCommissionRate(), seed.getEffectiveFrom());
    }
}
