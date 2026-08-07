package site.pointwalletservice.deposit.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.pointwalletservice.deposit.domain.Deposit;

public interface DepositJpaRepository extends JpaRepository<Deposit, Long> {
    Optional<Deposit> findByOrderId(String orderId);
}