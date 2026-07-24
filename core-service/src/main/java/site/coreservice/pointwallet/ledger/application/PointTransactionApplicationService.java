package site.coreservice.pointwallet.ledger.application;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;

@Service
@RequiredArgsConstructor
public class PointTransactionApplicationService implements PointTransactionService {

    private final PointTransactionRepository pointTransactionRepository;

    @Override
    @Transactional
    public void record(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId) {
        PointTransaction transaction = PointTransaction.record(walletId, type, amount, balanceAfter, relatedId);
        pointTransactionRepository.save(transaction);
    }
}