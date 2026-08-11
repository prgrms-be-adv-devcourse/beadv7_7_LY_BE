package site.pointwalletservice.wallet.domain;

import java.util.Optional;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findByUserId(Long userId);

    /** 잔액 변경 트랜잭션 전용 — NOWAIT 비관적 락으로 조회한다. 경합 시 즉시 실패한다. */
    Optional<Wallet> findByUserIdForUpdate(Long userId);
}