package site.pointwalletservice.wallet.deadletter.infrastructure;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.pointwalletservice.wallet.deadletter.domain.DeadLetterStatus;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetter;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetterRepository;

@Repository
@RequiredArgsConstructor
public class WithdrawFeeDeadLetterRepositoryImpl implements WithdrawFeeDeadLetterRepository {

    private final WithdrawFeeDeadLetterJpaRepository jpaRepository;

    @Override
    public WithdrawFeeDeadLetter save(WithdrawFeeDeadLetter deadLetter) {
        return jpaRepository.save(deadLetter);
    }

    @Override
    public Optional<WithdrawFeeDeadLetter> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<WithdrawFeeDeadLetter> findByStatusOrderByCreatedAtDesc(DeadLetterStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}