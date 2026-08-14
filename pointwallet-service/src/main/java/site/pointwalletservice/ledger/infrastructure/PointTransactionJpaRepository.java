package site.pointwalletservice.ledger.infrastructure;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.pointwalletservice.ledger.domain.PointTransaction;
import site.pointwalletservice.ledger.domain.PointTransactionType;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransaction, Long> {

    // 리턴 타입이 Page<T>라 Spring Data가 이 쿼리에서 count 쿼리를 자동 유도함 (join·group by 없는
    // 단순 where절이라 유도가 안정적으로 됨) — count 쿼리를 별도로 안 짜도 됨.
    @Query("""
            select t from PointTransaction t
            where t.walletId = :walletId
              and (:type is null or t.type = :type)
              and (:from is null or t.occurredAt >= :from)
              and (:to is null or t.occurredAt <= :to)
            order by t.occurredAt desc
            """)
    Page<PointTransaction> searchByWalletId(@Param("walletId") Long walletId,
                                            @Param("type") PointTransactionType type,
                                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                            Pageable pageable);

    // infrastructure/PointTransactionJpaRepository.java 에 추가
    boolean existsByRelatedIdAndType(Long relatedId, PointTransactionType type);
}