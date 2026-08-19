package site.pointwalletservice.ledger.application;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.ledger.application.dto.PointTransactionSearchResult;
import site.pointwalletservice.ledger.domain.PointTransaction;
import site.pointwalletservice.ledger.domain.PointTransactionRepository;
import site.pointwalletservice.ledger.domain.PointTransactionSearchPage;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.ledger.exception.LedgerErrorCode;
import site.pointwalletservice.ledger.exception.LedgerException;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletService;

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
    @Transactional
    public void recordForAuction(Long walletId, PointTransactionType type, Money amount, Money balanceAfter,
                                 Long relatedId, Long auctionId) {
        PointTransaction transaction = PointTransaction.recordForAuction(
                walletId, type, amount, balanceAfter, relatedId, auctionId);
        pointTransactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsForRelatedId(Long relatedId, PointTransactionType type) {
        return pointTransactionRepository.existsByRelatedIdAndType(relatedId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Money> findLatestHoldAmountByAuctionId(Long auctionId) {
        return pointTransactionRepository.findLatestByAuctionIdAndType(auctionId, PointTransactionType.HOLD)
                .map(PointTransaction::getAmount);
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