package site.coreservice.pointwallet.ledger.application;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.pointwallet.ledger.application.dto.PointTransactionSearchResult;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;
import site.coreservice.pointwallet.ledger.domain.PointTransactionSearchPage;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.ledger.exception.LedgerErrorCode;
import site.coreservice.pointwallet.ledger.exception.LedgerException;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.application.WalletService;

@Service
@RequiredArgsConstructor
public class PointTransactionApplicationService implements PointTransactionService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final PointTransactionRepository pointTransactionRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public void record(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId) {
        PointTransaction transaction = PointTransaction.record(walletId, type, amount, balanceAfter, relatedId);
        pointTransactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PointTransactionSearchResult findTransactions(Long userId, String rawType,
                                                         LocalDateTime from, LocalDateTime to, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clampSize(size);
        PointTransactionType type = parseType(rawType);
        validateDateRange(from, to);

        return walletService.findWalletId(userId)
                .map(walletId -> {
                    PointTransactionSearchPage searchPage =
                            pointTransactionRepository.search(walletId, type, from, to, safePage, safeSize);
                    return PointTransactionSearchResult.of(searchPage, safePage, safeSize);
                })
                .orElseGet(() -> PointTransactionSearchResult.empty(safePage, safeSize));
    }

    private PointTransactionType parseType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        try {
            return PointTransactionType.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            throw new LedgerException(LedgerErrorCode.INVALID_TRANSACTION_TYPE);
        }
    }

    private int clampSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new LedgerException(LedgerErrorCode.INVALID_DATE_RANGE);
        }
    }
}