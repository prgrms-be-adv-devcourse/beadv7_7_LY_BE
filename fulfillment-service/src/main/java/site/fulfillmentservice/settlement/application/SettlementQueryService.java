package site.fulfillmentservice.settlement.application;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.fulfillmentservice.settlement.application.dto.SettlementBatchResult;
import site.fulfillmentservice.settlement.application.dto.SettlementItemSearchResult;
import site.fulfillmentservice.settlement.domain.SettlementBatchRepository;
import site.fulfillmentservice.settlement.domain.SettlementItemRepository;
import site.fulfillmentservice.settlement.domain.SettlementItemSearchPage;
import site.fulfillmentservice.settlement.domain.SettlementStatus;
import site.fulfillmentservice.settlement.exception.SettlementErrorCode;
import site.fulfillmentservice.settlement.exception.SettlementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryService {

    private static final int MIN_SIZE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final SettlementItemRepository settlementItemRepository;
    private final SettlementBatchRepository settlementBatchRepository;

    public SettlementItemSearchResult findItems(Long sellerId, String rawStatus,
                                                 LocalDateTime from, LocalDateTime to, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clampSize(size);
        SettlementStatus status = parseStatus(rawStatus);
        validateDateRange(from, to);

        SettlementItemSearchPage searchPage = settlementItemRepository.search(sellerId, status, from, to, safePage, safeSize);
        return SettlementItemSearchResult.of(searchPage, safePage, safeSize);
    }

    public List<SettlementBatchResult> findBatches(Long sellerId) {
        return settlementBatchRepository.findAllBySellerId(sellerId).stream()
                .map(SettlementBatchResult::from)
                .toList();
    }

    private SettlementStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return SettlementStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new SettlementException(SettlementErrorCode.INVALID_STATUS);
        }
    }

    private int clampSize(int size) {
        if (size < MIN_SIZE) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new SettlementException(SettlementErrorCode.INVALID_DATE_RANGE);
        }
    }
}
