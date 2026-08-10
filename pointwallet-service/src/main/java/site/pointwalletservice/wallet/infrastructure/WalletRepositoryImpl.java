package site.pointwalletservice.wallet.infrastructure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.pointwalletservice.wallet.domain.Wallet;
import site.pointwalletservice.wallet.domain.WalletRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {

    private final WalletJpaRepository walletJpaRepository;

    @Override
    public Wallet save(Wallet wallet) {
        return walletJpaRepository.save(wallet);
    }

    @Override
    public Optional<Wallet> findByUserId(Long userId) {
        return walletJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Wallet> findByUserIdForUpdate(Long userId) {
        return walletJpaRepository.findByUserIdForUpdate(userId);
    }

    @Override
    public void setLockWaitTimeout(int seconds) {
        walletJpaRepository.setLockWaitTimeout(seconds);
    }
}