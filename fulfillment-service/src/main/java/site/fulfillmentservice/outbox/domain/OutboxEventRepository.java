package site.fulfillmentservice.outbox.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent outboxEvent);

    Optional<OutboxEvent> findById(Long id);

    List<OutboxEvent> findPendingOldestFirst(int limit);

    /** 재시도 한도를 넘긴 행은 markFailed()에서 이미 DEAD로 전환되므로 여기엔 잡히지 않는다. */
    List<OutboxEvent> findFailedOldestFirst(int limit);

    /** DEAD는 원인 조사가 끝나기 전까진 지우면 안 되므로 대상에서 제외한다. 반환값은 이번 호출에서 실제로 지운 건수. */
    int deletePublishedBefore(LocalDateTime cutoff, int limit);
}
