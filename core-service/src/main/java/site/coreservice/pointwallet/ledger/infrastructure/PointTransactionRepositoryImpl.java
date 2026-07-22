package site.coreservice.pointwallet.ledger.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;

@Repository
@RequiredArgsConstructor
public class PointTransactionRepositoryImpl implements PointTransactionRepository {

    private final PointTransactionJpaRepository pointTransactionJpaRepository;

    @Override
    public PointTransaction save(PointTransaction pointTransaction) {
        return pointTransactionJpaRepository.save(pointTransaction);
    }
}