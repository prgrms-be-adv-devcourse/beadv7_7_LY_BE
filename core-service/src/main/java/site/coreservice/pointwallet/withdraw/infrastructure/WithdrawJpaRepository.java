package site.coreservice.pointwallet.withdraw.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.pointwallet.withdraw.domain.Withdraw;

public interface WithdrawJpaRepository extends JpaRepository<Withdraw, Long> {
}