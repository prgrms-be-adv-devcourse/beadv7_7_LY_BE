package site.coreservice.pointwallet.wallet.application;
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
    public WalletBalanceResult deduct(Long userId, Money amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        // 잔액 부족 검증은 Wallet 스스로 하고 InsufficientBalanceException을 던진다 - 여기서 잡지 않고
        // 호출한 컨텍스트(Deposit, Hold 등)가 자기 ErrorCode로 번역하도록 그대로 전파한다.
        wallet.deduct(amount);
        walletRepository.save(wallet);

        return new WalletBalanceResult(wallet.getId(), wallet.getBalance());
    }
}