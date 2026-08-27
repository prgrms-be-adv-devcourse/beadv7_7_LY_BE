package site.pointwalletservice.withdraw.domain;

import java.util.Optional;

public interface WithdrawRepository {
    Withdraw save(Withdraw withdraw);
    Optional<Withdraw> findById(Long id);
    Optional<Withdraw> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}