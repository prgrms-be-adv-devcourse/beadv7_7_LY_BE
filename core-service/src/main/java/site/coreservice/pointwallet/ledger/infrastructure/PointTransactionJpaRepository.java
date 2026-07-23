package site.coreservice.pointwallet.ledger.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransaction, Long> {}