package site.coreservice.pointwallet.ledger.application;

import java.time.LocalDateTime;
import site.coreservice.pointwallet.ledger.application.dto.PointTransactionSearchResult;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;

public interface PointTransactionService {

    void record(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId);

    PointTransactionSearchResult findTransactions(Long userId, String rawType,
                                                  LocalDateTime from, LocalDateTime to, int page, int size);
}