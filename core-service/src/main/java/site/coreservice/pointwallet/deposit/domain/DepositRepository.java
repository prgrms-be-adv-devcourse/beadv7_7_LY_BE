package site.coreservice.pointwallet.deposit.domain;

import java.util.Optional;

public interface DepositRepository {

    Deposit save(Deposit deposit);

    Optional<Deposit> findByOrderId(String orderId);

    Optional<Deposit> findById(Long id);
}