package site.pointwalletservice.wallet.deadletter.infrastructure;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.pointwalletservice.wallet.deadletter.domain.DeadLetterStatus;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetter;

public interface WithdrawFeeDeadLetterJpaRepository extends JpaRepository<WithdrawFeeDeadLetter, Long> {
    List<WithdrawFeeDeadLetter> findByStatusOrderByCreatedAtDesc(DeadLetterStatus status);
}