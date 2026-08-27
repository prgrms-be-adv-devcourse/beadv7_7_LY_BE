package site.pointwalletservice.withdraw.infrastructure;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.pointwalletservice.withdraw.domain.Withdraw;
import site.pointwalletservice.withdraw.domain.WithdrawRepository;

@Repository
@RequiredArgsConstructor
public class WithdrawRepositoryImpl implements WithdrawRepository {

    private final WithdrawJpaRepository withdrawJpaRepository;

    @Override
    public Withdraw save(Withdraw withdraw) {
        return withdrawJpaRepository.save(withdraw);
    }

    @Override
    public Optional<Withdraw> findById(Long id) {
        return withdrawJpaRepository.findById(id);
    }

    @Override
    public Optional<Withdraw> findByIdempotencyKey(String idempotencyKey) {
        return withdrawJpaRepository.findByIdempotencyKey(idempotencyKey);
    }
}