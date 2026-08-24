package site.fulfillmentservice.outbox.domain;

import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent outboxEvent);

    Optional<OutboxEvent> findById(Long id);

    List<OutboxEvent> findPendingOldestFirst(int limit);

    /** 재시도 한도를 넘긴 행은 markFailed()에서 이미 DEAD로 전환되므로 여기엔 잡히지 않는다. */
    List<OutboxEvent> findFailedOldestFirst(int limit);
}
