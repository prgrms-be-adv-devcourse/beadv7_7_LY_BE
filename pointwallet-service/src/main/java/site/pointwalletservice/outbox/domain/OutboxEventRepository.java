// outbox/domain/OutboxEventRepository.java
package site.pointwalletservice.outbox.domain;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent outboxEvent);

    Optional<OutboxEvent> findById(Long id);

    /** PENDING 상태 행을 오래된 순으로 최대 limit개 조회 — 폴링용. */
    List<OutboxEvent> findPendingOldestFirst(int limit);

    /** FAILED 상태(=재시도 대상) 행을 오래된 순으로 최대 limit개 조회 — 재시도 폴링용.
     *  재시도 한도를 넘긴 행은 markFailed()에서 이미 DEAD로 전환되므로 여기엔 잡히지 않는다. */
    List<OutboxEvent> findFailedOldestFirst(int limit);

    /** 관리자 목록 조회용 - DEAD 상태 행을 최신순으로. */
    List<OutboxEvent> findDeadNewestFirst();
}