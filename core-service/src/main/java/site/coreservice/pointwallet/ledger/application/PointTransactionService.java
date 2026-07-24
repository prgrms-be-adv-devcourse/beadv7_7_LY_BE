package site.coreservice.pointwallet.ledger.application;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;

public interface PointTransactionService {
    void record(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId);
}