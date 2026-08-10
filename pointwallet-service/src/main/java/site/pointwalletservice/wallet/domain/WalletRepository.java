package site.pointwalletservice.wallet.domain;

import java.util.Optional;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findByUserId(Long userId);

    /** 잔액 변경 트랜잭션 전용 — 비관적 락으로 조회한다. */
    Optional<Wallet> findByUserIdForUpdate(Long userId);

    /** findByUserIdForUpdate() 직전에 호출 — 이 세션의 락 대기 상한선을 짧게 줄인다. */
    void setLockWaitTimeout(int seconds);
}