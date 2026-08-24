package site.fulfillmentservice.outbox.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "outbox_event",
        indexes = @Index(name = "idx_outbox_event_status_created_at", columnList = "status, created_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    public static final int MAX_RETRY_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    /** 원본 이벤트 클래스의 FQCN이어야 한다. */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    /** 폴링 순서(오래된 순)의 기준. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 실제로 Kafka 발행에 성공한 시각. createdAt과 의도적으로 분리했다 - 재시도가 있었다면
     *  그 차이가 곧 "발행이 얼마나 지연됐는지"를 보여준다. */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** 발행 실패 사유 — status가 FAILED/DEAD일 때만 값이 있다. 원인 추적용. */
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    private OutboxEvent(String topic, String partitionKey, String eventType, String payload) {
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static OutboxEvent create(String topic, String partitionKey, String eventType, String payload) {
        return new OutboxEvent(topic, partitionKey, eventType, payload);
    }

    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.retryCount++;
        this.lastError = reason;
        this.status = (this.retryCount >= MAX_RETRY_COUNT)
                ? OutboxEventStatus.DEAD
                : OutboxEventStatus.FAILED;
    }
}
