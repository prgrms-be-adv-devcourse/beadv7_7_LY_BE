package site.coreservice.pointwallet.deposit.infrastructure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.pointwallet.deposit.domain.Deposit;
import site.coreservice.pointwallet.deposit.domain.DepositRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DepositRepositoryImpl implements DepositRepository {

    private final DepositJpaRepository depositJpaRepository;

    @Override
    public Deposit save(Deposit deposit) {
        return depositJpaRepository.save(deposit);
    }

    @Override
    public Optional<Deposit> findByOrderId(String orderId) {
        return depositJpaRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Deposit> findById(Long id) {
        return depositJpaRepository.findById(id);
    }
}