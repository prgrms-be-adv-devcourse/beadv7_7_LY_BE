// outbox/domain/OutboxEvent.java
package site.pointwalletservice.outbox.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DB 커밋과 이벤트 발행 사이의 원자성을 보장하기 위한 아웃박스. 비즈니스 트랜잭션(예: 지갑 차감)과
 * 같은 트랜잭션 안에서 이 행을 같이 저장하면, 커밋이 성공한 이벤트만 여기 남는다. 실제 Kafka
 * 발행은 이 트랜잭션과 무관하게 별도 스케줄러(OutboxRelay)가 이 테이블을 폴링하며 처리한다 -
 * 그래서 "DB는 커밋됐는데 이벤트 발행 직전에 프로세스가 죽는" 케이스에서도, 재기동 후 스케줄러가
 * 미발행 행을 다시 집어서 발행한다.
 * <p>
 * 특정 이벤트 하나 전용이 아니라 범용으로 만들었다 - eventType에 원본 이벤트 클래스의 FQCN을
 * 저장해두면, 발행 시 그 클래스로 역직렬화해서 원래 발행하려던 것과 동일한 타입의 객체를
 * Kafka로 보낼 수 있다.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    /** 원본 이벤트 클래스의 FQCN — 발행 시 이 클래스로 역직렬화한다. */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** 원본 이벤트를 Jackson으로 직렬화한 JSON. */
    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** 발행 실패 사유 — status=FAILED일 때만 값이 있다. 원인 추적용. */
    @Column(name = "last_error")
    private String lastError;

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
        this.status = OutboxEventStatus.FAILED;
        this.lastError = reason;
    }
}