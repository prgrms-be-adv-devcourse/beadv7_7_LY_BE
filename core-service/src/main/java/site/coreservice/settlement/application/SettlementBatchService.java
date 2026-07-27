package site.coreservice.settlement.application;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.settlement.domain.SettlementBatch;
import site.coreservice.settlement.domain.SettlementBatchRepository;
import site.coreservice.settlement.domain.SettlementItem;
import site.coreservice.settlement.domain.SettlementItemRepository;
import site.coreservice.settlement.domain.SettlementStatus;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettlementBatchService {

    private final SettlementItemRepository settlementItemRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final SettlementEventPublisher settlementEventPublisher;

    @Transactional(readOnly = true)
    public List<Long> findEligibleSellerIds(LocalDateTime periodTo) {
        return settlementItemRepository.findAllByStatusAndCompletedAtBefore(SettlementStatus.PENDING, periodTo).stream()
            .map(SettlementItem::getSellerId)
            .distinct()
            .toList();
    }

    public void createBatchForSeller(Long sellerId, LocalDateTime periodFrom, LocalDateTime periodTo, LocalDateTime confirmedAt) {
        if (settlementBatchRepository.existsBySellerIdAndPeriodFromAndPeriodTo(sellerId, periodFrom, periodTo)) {
            log.info("이미 이 기간에 정산 배치가 생성된 판매자입니다. 중복 처리로 건너뜁니다. sellerId={}", sellerId);
            return;
        }

        List<SettlementItem> items = settlementItemRepository
            .findAllByStatusAndCompletedAtBeforeAndSellerId(SettlementStatus.PENDING, periodTo, sellerId);
        if (items.isEmpty()) {
            log.info("정산 대상 PENDING 항목이 없어 배치 생성을 건너뜁니다. sellerId={}", sellerId);
            return;
        }

        SettlementBatch batch = SettlementBatch.of(sellerId, items, periodFrom, periodTo, confirmedAt);

        SettlementBatch savedBatch;
        try {
            savedBatch = settlementBatchRepository.save(batch);
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 이 기간에 정산 배치가 생성된 판매자입니다(동시성). 중복 처리로 건너뜁니다. sellerId={}", sellerId);
            return;
        }

        items.forEach(item -> item.markPaid(savedBatch.getId(), confirmedAt));

        settlementEventPublisher.publishConfirmed(savedBatch);
        log.info("정산 배치 생성 완료: sellerId={}, batchId={}, itemCount={}", sellerId, savedBatch.getId(), items.size());
    }
}
