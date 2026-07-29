package site.coreservice.settlement.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementBatchScheduler {

    private static final int SETTLEMENT_DAY_OF_MONTH = 25;

    private final SettlementBatchService settlementBatchService;

    @Scheduled(fixedDelay = 1000 * 10)
    public void createSettlementBatches() {
        LocalDateTime periodTo = currentPeriodTo();
        LocalDateTime periodFrom = periodTo.minusMonths(1);

        List<Long> sellerIds = settlementBatchService.findEligibleSellerIds(periodTo);
        for (Long sellerId : sellerIds) {
            try {
                settlementBatchService.createBatchForSeller(sellerId, periodFrom, periodTo, LocalDateTime.now());
            } catch (Exception e) {
                log.error("정산 배치 생성 실패: sellerId={}", sellerId, e);
            }
        }
    }

    private LocalDateTime currentPeriodTo() {
        return LocalDateTime.now();
        //return LocalDate.now().withDayOfMonth(SETTLEMENT_DAY_OF_MONTH).atStartOfDay();
    }
}
