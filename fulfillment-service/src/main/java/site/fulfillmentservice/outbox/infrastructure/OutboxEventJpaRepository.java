package site.fulfillmentservice.outbox.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.fulfillmentservice.outbox.domain.OutboxEvent;
import site.fulfillmentservice.outbox.domain.OutboxEventStatus;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);

    /**
     * 대량 삭제 시 락을 오래 잡지 않으려고 배치 크기를 제한한다.
     * <p>
     * clearAutomatically = true — 네이티브 삭제는 영속성 컨텍스트를 거치지 않아서, 안 비우면 같은 트랜잭션
     * 안의 이후 조회가 이미 지워진 행을 1차 캐시에서 그대로 돌려준다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM fulfillment_outbox_event WHERE status = 'PUBLISHED' AND published_at < :cutoff LIMIT :limit",
            nativeQuery = true)
    int deletePublishedBefore(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
