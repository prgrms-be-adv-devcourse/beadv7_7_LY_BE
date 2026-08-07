package site.pointwalletservice.wallet.domain;

import java.util.Optional;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findByUserId(Long userId);
}