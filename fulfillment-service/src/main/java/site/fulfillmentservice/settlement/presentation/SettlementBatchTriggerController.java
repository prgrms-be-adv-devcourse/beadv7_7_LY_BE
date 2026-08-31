package site.fulfillmentservice.settlement.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.fulfillmentservice.settlement.application.SettlementBatchScheduler;

/**
 * 정산 배치 스케줄러(25~28일 cron) 수동 트리거 (local 전용 — 프로파일 밖에서는 빈이 뜨지 않아 경로 자체가 없다).
 * 스케줄러의 날짜 계산 로직을 그대로 재사용하므로, 오늘이 몇 일이든 이번 달 25일 이전에
 * completedAt된 PENDING 항목들이 정상적으로 배치 대상이 된다.
 */
@Profile("local")
@ConditionalOnProperty(name = "settlement.batch-trigger.enabled", havingValue = "true")
@RestController
@RequiredArgsConstructor
public class SettlementBatchTriggerController {

    private final SettlementBatchScheduler settlementBatchScheduler;

    @PostMapping("/internal/v1/settlement-batches/run")
    public ApiResponse<Void> run() {
        settlementBatchScheduler.createSettlementBatches();
        return ApiResponse.success();
    }
}
