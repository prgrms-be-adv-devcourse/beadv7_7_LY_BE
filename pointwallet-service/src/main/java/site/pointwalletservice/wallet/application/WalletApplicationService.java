package site.pointwalletservice.wallet.application;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.domain.Wallet;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;
import site.pointwalletservice.wallet.domain.WalletRepository;

@Service
@RequiredArgsConstructor
public class WalletApplicationService implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public WalletBalanceResult charge(Long userId, Money amount) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(Wallet.open(userId)));

        wallet.charge(amount);
        walletRepository.save(wallet);

        return new WalletBalanceResult(wallet.getId(), wallet.getBalance());
    }

    @Override
    @Transactional
    public WalletBalanceResult credit(Long userId, Money amount) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(WalletNotFoundException::new);

        wallet.charge(amount);
        walletRepository.save(wallet);

        return new WalletBalanceResult(wallet.getId(), wallet.getBalance());
    }

    @Override
    @Transactional
    public WalletBalanceResult deduct(Long userId, Money amount) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
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

    @Override
    @Transactional
    public void lockForUpdate(Long userId) {
        walletRepository.findByUserIdForUpdate(userId);
    }

    @Override
    @Transactional
    public void lockForUpdate(Long userId, Long secondUserId) {
        if (secondUserId == null || secondUserId.equals(userId)) {
            lockForUpdate(userId);
            return;
        }
        Long smaller = Math.min(userId, secondUserId);
        Long larger = Math.max(userId, secondUserId);
        walletRepository.findByUserIdForUpdate(smaller);
        walletRepository.findByUserIdForUpdate(larger);
    }
}