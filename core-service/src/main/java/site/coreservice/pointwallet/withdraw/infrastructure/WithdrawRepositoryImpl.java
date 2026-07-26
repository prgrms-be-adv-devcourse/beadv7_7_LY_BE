package site.coreservice.pointwallet.withdraw.infrastructure;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.pointwallet.withdraw.domain.Withdraw;
import site.coreservice.pointwallet.withdraw.domain.WithdrawRepository;

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
}