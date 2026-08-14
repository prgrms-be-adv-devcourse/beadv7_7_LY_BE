// outbox/infrastructure/OutboxEventRepositoryImpl.java
package site.pointwalletservice.outbox.infrastructure;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import site.pointwalletservice.outbox.domain.OutboxEventStatus;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return outboxEventJpaRepository.save(outboxEvent);
    }

    @Override
    public List<OutboxEvent> findPendingOldestFirst(int limit) {
        return outboxEventJpaRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING, PageRequest.of(0, limit));
    }
}