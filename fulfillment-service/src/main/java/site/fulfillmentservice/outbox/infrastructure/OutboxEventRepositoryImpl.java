package site.fulfillmentservice.outbox.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import site.fulfillmentservice.outbox.domain.OutboxEvent;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;
import site.fulfillmentservice.outbox.domain.OutboxEventStatus;

@Repository
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    public OutboxEventRepositoryImpl(final OutboxEventJpaRepository outboxEventJpaRepository) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
    }

    @Override
    public OutboxEvent save(final OutboxEvent outboxEvent) {
        return outboxEventJpaRepository.save(outboxEvent);
    }

    @Override
    public Optional<OutboxEvent> findById(final Long id) {
        return outboxEventJpaRepository.findById(id);
    }

    @Override
    public List<OutboxEvent> findPendingOldestFirst(final int limit) {
        return outboxEventJpaRepository.findByStatusOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING, PageRequest.of(0, limit));
    }

    @Override
    public List<OutboxEvent> findFailedOldestFirst(final int limit) {
        return outboxEventJpaRepository.findByStatusOrderByCreatedAtAsc(
            OutboxEventStatus.FAILED, PageRequest.of(0, limit));
    }

    /** 배치 반복문에서 호출당 하나씩 짧은 트랜잭션을 갖게 하려고 메서드 단위로 건다(스케줄러엔 안 걺). */
    @Override
    @Transactional
    public int deletePublishedBefore(final LocalDateTime cutoff, final int limit) {
        return outboxEventJpaRepository.deletePublishedBefore(cutoff, limit);
    }
}
