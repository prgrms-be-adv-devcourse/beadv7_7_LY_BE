// outbox/domain/OutboxEvent.java
package site.pointwalletservice.outbox.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.pointwalletservice.outbox.exception.OutboxErrorCode;
import site.pointwalletservice.outbox.exception.OutboxException;

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
 * <p>
 * FAILED는 종착 상태가 아니다 - OutboxRelay가 PENDING과 함께 FAILED 행도 폴링 대상에 포함해
 * 재시도한다. 재시도가 {@link #MAX_RETRY_COUNT}회를 넘으면 더 이상 자동으로 집히지 않도록
 * DEAD로 전환하고, 이후는 운영자가 last_error를 보고 수동으로 판단한다(원인 자체가 영구적인
 * 실패라면 재시도해도 계속 실패하기 때문).
 */
@Entity
@Table(
        name = "outbox_event",
        indexes = @Index(name = "idx_outbox_event_status_created_at", columnList = "status, created_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    /** 이 횟수만큼 실패하면 더 이상 재시도하지 않고 DEAD로 전환한다. */
    public static final int MAX_RETRY_COUNT = 5;

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

    /** 이 아웃박스 행이 생성된 시각 - 사실상 이벤트가 발생한 시각과 같고, 폴링 순서(오래된 순)의 기준이다. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 실제로 Kafka 발행에 성공한 시각. createdAt과 의도적으로 분리했다 - 재시도가 있었다면
     *  그 차이가 곧 "발행이 얼마나 지연됐는지"를 보여준다. */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** 발행 실패 사유 — status가 FAILED/DEAD일 때만 값이 있다. 원인 추적용. */
    @Column(name = "last_error")
    private String lastError;

    /** markFailed()가 호출된 누적 횟수. MAX_RETRY_COUNT에 도달하면 DEAD로 전환된다. */
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

    /**
     * 관리자가 "원인이 해소됐다"고 판단했을 때 DEAD를 PENDING으로 되돌려 다음 폴링에 다시 태운다.
     * retryCount를 0으로 리셋하므로, 이번에도 실패하면 다시 MAX_RETRY_COUNT번의 기회를 새로 받는다 -
     * 그래서 "사람이 판단해서 되돌렸는데 또 5번 다 까먹고 DEAD로 돌아간다"는 게 이상하지 않다.
     * DEAD가 아닌 상태에서는 호출할 이유가 없다 - PENDING/FAILED는 이미 자동으로 돌고 있고,
     * PUBLISHED는 이미 끝난 건이라 되돌릴 대상이 아니다.
     */
    public void retryManually() {
        if (this.status != OutboxEventStatus.DEAD) {
            throw new OutboxException(OutboxErrorCode.INVALID_STATUS_FOR_RETRY);
        }
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
    }
}