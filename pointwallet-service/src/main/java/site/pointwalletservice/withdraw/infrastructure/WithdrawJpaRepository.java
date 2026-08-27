package site.pointwalletservice.withdraw.infrastructure;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.pointwalletservice.withdraw.domain.Withdraw;

public interface WithdrawJpaRepository extends JpaRepository<Withdraw, Long> {
    Optional<Withdraw> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}