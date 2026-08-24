package site.fulfillmentservice.outbox.domain;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    /** 발행 실패, 재시도 대상 */
    FAILED,
    /** 재시도 한도(OutboxEvent.MAX_RETRY_COUNT)를 넘겨 더 이상 자동 재시도되지 않는 상태 */
    DEAD
}
