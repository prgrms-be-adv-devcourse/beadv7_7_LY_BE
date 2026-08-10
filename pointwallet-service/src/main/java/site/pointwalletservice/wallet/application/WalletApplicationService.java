package site.pointwalletservice.wallet.application;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.domain.Wallet;
import site.pointwalletservice.wallet.exception.WalletLockFailedException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;
import site.pointwalletservice.wallet.domain.WalletRepository;

@Service
@RequiredArgsConstructor
public class WalletApplicationService implements WalletService {

    private final WalletRepository walletRepository;

    /**
     * 지갑 락은 홀드 행 락과 다르게 기본 블로킹(대기)이다 - 같은 유저가 서로 다른 두 경매에 동시에
     * 입찰하는 것도 정책상 정상이라, 지갑 레벨 경합은 순서대로 기다리면 둘 다 성공해야 하는 상황이지
     * 즉시 실패시킬 대상이 아니다(WalletJpaRepository.findByUserIdForUpdate 주석 참고). 그래도 만약
     * 정말 락을 못 잡고 끝내 실패하면(타임아웃 등 - 정상 흐름에서는 거의 안 일어남) Spring이
     * PessimisticLockingFailureException으로 던진다 - 그대로 두면 GlobalExceptionHandler의
     * catch-all(GERR-0001, "예기치 못한 서버 오류")로 뭉개져서 로그에도 에러로 찍히고 클라이언트도
     * 원인을 알 수 없다. 여기서 WalletLockFailedException으로 번역해서 명확하게 만든다.
     */
    private Optional<Wallet> findByUserIdForUpdate(Long userId) {
        try {
            return walletRepository.findByUserIdForUpdate(userId);
        } catch (PessimisticLockingFailureException e) {
            throw new WalletLockFailedException();
        }
    }

    @Override
    @Transactional
    public WalletBalanceResult charge(Long userId, Money amount) {
        Wallet wallet = findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(Wallet.open(userId)));

        wallet.charge(amount);
        walletRepository.save(wallet);

        return new WalletBalanceResult(wallet.getId(), wallet.getBalance());
    }

    @Override
    @Transactional
    public WalletBalanceResult credit(Long userId, Money amount) {
        Wallet wallet = findByUserIdForUpdate(userId)
                .orElseThrow(WalletNotFoundException::new);

        wallet.charge(amount);
        walletRepository.save(wallet);

        return new WalletBalanceResult(wallet.getId(), wallet.getBalance());
    }

    @Override
    @Transactional
    public WalletBalanceResult deduct(Long userId, Money amount) {
        Wallet wallet = findByUserIdForUpdate(userId)
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
    // 반드시 기존 트랜잭션 안에서만 호출돼야 한다 - 트랜잭션 없이(또는 REQUIRED로) 단독 호출하면
    // 이 메서드가 자기 트랜잭션을 열고 즉시 커밋하면서 락을 곧바로 풀어버려, 호출자는 잠긴 줄 알고
    // 진행하지만 실제로는 아무 보호도 없는 상태가 된다. MANDATORY면 그런 잘못된 호출이 예외로 바로 드러난다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockForUpdate(Long userId) {
        findByUserIdForUpdate(userId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockForUpdate(Long userId, Long secondUserId) {
        if (secondUserId == null || secondUserId.equals(userId)) {
            lockForUpdate(userId);
            return;
        }
        Long smaller = Math.min(userId, secondUserId);
        Long larger = Math.max(userId, secondUserId);
        findByUserIdForUpdate(smaller);
        findByUserIdForUpdate(larger);
    }
}