package site.fulfillmentservice.outbox.infrastructure;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private static final int RETENTION_DAYS = 7;
    private static final int BATCH_SIZE = 500;

    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(cron = "${outbox.cleanup.cron:0 0 3 * * *}")
    public void deletePublishedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int total = 0;
        int deleted;
        do {
            deleted = outboxEventRepository.deletePublishedBefore(cutoff, BATCH_SIZE);
            total += deleted;
        } while (deleted == BATCH_SIZE);

        log.info("Outbox PUBLISHED 정리 완료 — 총 {}건 삭제 (cutoff={})", total, cutoff);
    }
}
