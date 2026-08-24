package site.fulfillmentservice.outbox.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import site.fulfillmentservice.outbox.domain.OutboxEvent;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;
import site.fulfillmentservice.outbox.domain.OutboxEventStatus;
import site.fulfillmentservice.support.RepositoryTest;

@RepositoryTest
@Import(OutboxEventRepositoryImpl.class)
class OutboxEventRepositoryImplTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    private final LocalDateTime cutoff = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    private OutboxEvent published(LocalDateTime publishedAt) {
        OutboxEvent event = OutboxEvent.create("order.completed", "1", "some.EventType", "{}");
        event.markPublished();
        ReflectionTestUtils.setField(event, "publishedAt", publishedAt);
        return outboxEventJpaRepository.save(event);
    }

    private OutboxEvent withStatus(OutboxEventStatus status) {
        OutboxEvent event = OutboxEvent.create("order.completed", "1", "some.EventType", "{}");
        ReflectionTestUtils.setField(event, "status", status);
        ReflectionTestUtils.setField(event, "publishedAt", cutoff.minusDays(1));
        return outboxEventJpaRepository.save(event);
    }

    @Test
    void deletePublishedBefore_cutoff_이전에_발행된_PUBLISHED_행만_삭제한다() {
        OutboxEvent old = published(cutoff.minusDays(1));
        OutboxEvent recent = published(cutoff.plusDays(1));

        int deleted = outboxEventRepository.deletePublishedBefore(cutoff, 100);

        assertThat(deleted).isEqualTo(1);
        assertThat(outboxEventJpaRepository.findById(old.getId())).isEmpty();
        assertThat(outboxEventJpaRepository.findById(recent.getId())).isPresent();
    }

    @Test
    void deletePublishedBefore_PUBLISHED가_아닌_상태는_지우지_않는다() {
        OutboxEvent pending = withStatus(OutboxEventStatus.PENDING);
        OutboxEvent dead = withStatus(OutboxEventStatus.DEAD);

        int deleted = outboxEventRepository.deletePublishedBefore(cutoff, 100);

        assertThat(deleted).isEqualTo(0);
        assertThat(outboxEventJpaRepository.findById(pending.getId())).isPresent();
        assertThat(outboxEventJpaRepository.findById(dead.getId())).isPresent();
    }

    @Test
    void deletePublishedBefore_limit만큼만_지운다() {
        for (int i = 0; i < 5; i++) {
            published(cutoff.minusDays(1));
        }

        int deleted = outboxEventRepository.deletePublishedBefore(cutoff, 3);

        assertThat(deleted).isEqualTo(3);
    }
}
