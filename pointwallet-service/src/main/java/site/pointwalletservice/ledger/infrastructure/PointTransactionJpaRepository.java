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

    java.util.Optional<PointTransaction> findFirstByAuctionIdAndTypeOrderByOccurredAtDesc(
            Long auctionId, PointTransactionType type);

    /**
     * (relatedId, type) 조합으로 원장에 남은 auctionId를 되짚는다. Hold 롤백(보상 트랜잭션)이
     * auctionId 없이 holdId만 받았을 때, Hold 행이 이미 삭제됐어도(교체/소멸) 원장만으로
     * 어느 경매의 홀드였는지 재구성하기 위한 용도 — uk_point_transaction_related_id_type
     * 제약 덕에 (relatedId, type) 조합은 최대 1건이라 findFirst 없이 findBy로 충분하다.
     * existsByRelatedIdAndType과 파라미터를 맞춰서, 호출부에서 PointTransactionType.HOLD를 넘긴다.
     */
    @Query("select t.auctionId from PointTransaction t where t.relatedId = :relatedId and t.type = :type")
    java.util.Optional<Long> findAuctionIdByRelatedIdAndType(@Param("relatedId") Long relatedId,
                                                             @Param("type") PointTransactionType type);
}