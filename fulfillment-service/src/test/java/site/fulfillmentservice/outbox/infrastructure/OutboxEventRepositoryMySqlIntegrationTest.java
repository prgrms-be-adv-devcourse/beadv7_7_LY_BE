package site.fulfillmentservice.outbox.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import site.fulfillmentservice.outbox.domain.OutboxEvent;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;

/**
 * H2는 "MySQL 호환 모드"일 뿐 완전히 동일하진 않아서, deletePublishedBefore의 네이티브
 * DELETE ... LIMIT 쿼리가 실제 로컬 MySQL에서도 그대로 동작하는지 확인한다.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("local")
class OutboxEventRepositoryMySqlIntegrationTest {

    private static final String TEST_TOPIC = "test.outbox-cleanup-verification";

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @AfterEach
    void tearDown() {
        outboxEventJpaRepository.findAll().stream()
            .filter(event -> TEST_TOPIC.equals(event.getTopic()))
            .forEach(outboxEventJpaRepository::delete);
    }

    private OutboxEvent published(LocalDateTime publishedAt) {
        OutboxEvent event = OutboxEvent.create(TEST_TOPIC, "1", "some.EventType", "{}");
        event.markPublished();
        ReflectionTestUtils.setField(event, "publishedAt", publishedAt);
        return outboxEventJpaRepository.save(event);
    }

    @Test
    void deletePublishedBefore_실제_MySQL에서_LIMIT_문법이_정상_동작한다() {
        // given
        LocalDateTime cutoff = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OutboxEvent old = published(cutoff.minusDays(1));
        OutboxEvent recent = published(cutoff.plusDays(1));

        // when
        int deleted = outboxEventRepository.deletePublishedBefore(cutoff, 100);

        // then
        assertThat(deleted).isEqualTo(1);
        assertThat(outboxEventJpaRepository.findById(old.getId())).isEmpty();
        assertThat(outboxEventJpaRepository.findById(recent.getId())).isPresent();
    }
}
