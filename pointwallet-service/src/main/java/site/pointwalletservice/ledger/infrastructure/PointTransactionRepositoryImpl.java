package site.pointwalletservice.ledger.infrastructure;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import site.pointwalletservice.ledger.domain.PointTransaction;
import site.pointwalletservice.ledger.domain.PointTransactionRepository;
import site.pointwalletservice.ledger.domain.PointTransactionSearchPage;
import site.pointwalletservice.ledger.domain.PointTransactionType;

@Repository
@RequiredArgsConstructor
public class PointTransactionRepositoryImpl implements PointTransactionRepository {

    private final PointTransactionJpaRepository pointTransactionJpaRepository;

    @Override
    public PointTransaction save(PointTransaction pointTransaction) {
        return pointTransactionJpaRepository.save(pointTransaction);
    }

    @Override
    public PointTransactionSearchPage search(Long walletId, PointTransactionType type,
                                             LocalDateTime from, LocalDateTime to, int page, int size) {
        Page<PointTransaction> result = pointTransactionJpaRepository.searchByWalletId(
                walletId, type, from, to, PageRequest.of(page, size));
        return new PointTransactionSearchPage(result.getContent(), result.getTotalElements());
    }
}