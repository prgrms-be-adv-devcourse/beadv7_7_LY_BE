package site.pointwalletservice.wallet.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.pointwalletservice.wallet.domain.Wallet;

public interface WalletJpaRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
}