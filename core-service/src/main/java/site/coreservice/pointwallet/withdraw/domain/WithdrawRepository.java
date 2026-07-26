package site.coreservice.pointwallet.withdraw.domain;

import java.util.Optional;

public interface WithdrawRepository {
    Withdraw save(Withdraw withdraw);
    Optional<Withdraw> findById(Long id);
}