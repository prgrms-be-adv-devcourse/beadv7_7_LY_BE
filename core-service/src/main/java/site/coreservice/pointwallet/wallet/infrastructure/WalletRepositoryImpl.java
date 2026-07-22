package site.coreservice.pointwallet.wallet.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.pointwallet.wallet.domain.Wallet;
import site.coreservice.pointwallet.wallet.domain.WalletRepository;

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
}