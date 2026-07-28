package site.coreservice.settlement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.settlement.domain.SettlementBatch;

public interface SettlementBatchJpaRepository extends JpaRepository<SettlementBatch, Long> {

    boolean existsBySellerIdAndPeriodFromAndPeriodTo(Long sellerId, LocalDateTime periodFrom, LocalDateTime periodTo);

    List<SettlementBatch> findAllBySellerIdOrderByConfirmedAtDesc(Long sellerId);
}
