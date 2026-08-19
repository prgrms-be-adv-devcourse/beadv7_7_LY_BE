// outbox/domain/OutboxEventStatus.java
package site.pointwalletservice.outbox.domain;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    /** 발행 실패, 재시도 대상(OutboxRelay가 PENDING과 함께 다시 집어간다). */
    FAILED,
    /** 재시도 한도(OutboxEvent.MAX_RETRY_COUNT)를 넘겨 더 이상 자동 재시도되지 않는 상태 — 수동 확인 필요. */
    DEAD
}