// outbox/domain/OutboxEventRepository.java
package site.pointwalletservice.outbox.domain;
import java.util.List;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent outboxEvent);

    /** PENDING 상태 행을 오래된 순으로 최대 limit개 조회 — 폴링용. */
    List<OutboxEvent> findPendingOldestFirst(int limit);
}