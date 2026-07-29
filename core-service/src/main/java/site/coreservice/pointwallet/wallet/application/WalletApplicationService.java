package site.coreservice.pointwallet.wallet.application;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.domain.Wallet;
import site.coreservice.pointwallet.wallet.exception.WalletNotFoundException;
import site.coreservice.pointwallet.wallet.domain.WalletRepository;

@Service
@RequiredArgsConstructor
public class WalletApplicationService implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public WalletBalanceResult charge(Long userId, Money amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.open(userId)));

        wallet.charge(amount);
        walletRepository.save(wallet);

        return new WalletBalanceResult(wallet.getId(), wallet.getBalance());
    }

    @Override
    @Transactional
    public WalletBalanceResult credit(Long userId, Money amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        wallet.charge(amount);
        walletRepository.save(wallet);

        return new WalletBalanceResult(wallet.getId(), wallet.getBalance());
    }

    @Override
    @Transactional
    public WalletBalanceResult deduct(Long userId, Money amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        wallet.deduct(amount);
        walletRepository.save(wallet);

        return new WalletBalanceResult(wallet.getId(), wallet.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findWalletId(Long userId) {
        return walletRepository.findByUserId(userId).map(Wallet::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public Money getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(Money.zero());
    }
}