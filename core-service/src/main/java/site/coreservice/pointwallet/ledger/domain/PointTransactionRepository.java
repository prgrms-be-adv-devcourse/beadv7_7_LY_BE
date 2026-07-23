package site.coreservice.pointwallet.ledger.domain;

public interface PointTransactionRepository {
    PointTransaction save(PointTransaction pointTransaction);
}