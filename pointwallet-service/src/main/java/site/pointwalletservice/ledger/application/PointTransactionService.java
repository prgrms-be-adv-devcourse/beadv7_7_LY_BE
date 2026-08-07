package site.pointwalletservice.ledger.application;

import java.time.LocalDateTime;
import site.pointwalletservice.ledger.application.dto.PointTransactionSearchResult;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;

public interface PointTransactionService {

    void record(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId);

    PointTransactionSearchResult findTransactions(Long userId, String rawType,
                                                  LocalDateTime from, LocalDateTime to, int page, int size);
}