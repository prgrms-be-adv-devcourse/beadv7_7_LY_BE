package site.pointwalletservice.deposit.reconciliation.application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import site.pointwalletservice.deposit.domain.PaymentGatewayClient;
import site.pointwalletservice.deposit.domain.PgInquiryResult;
import site.pointwalletservice.deposit.reconciliation.domain.DepositReconciliationLog;
import site.pointwalletservice.deposit.reconciliation.domain.DepositReconciliationLogRepository;
import site.pointwalletservice.deposit.reconciliation.domain.ReconciliationFailureType;

/**
 * DepositApplicationService의 보정 실패 catch 블록에서 호출한다. 기존엔 log.error 몇 줄로
 * 끝났는데(휘발성 - 로그가 밀려나면 끝), 이걸 DB 행으로 남겨서 관리자가 조회/조치할 수 있게 한다.
 * PG 조회(inquire)까지 실패 시점에 같이 해서 스냅샷으로 남기므로, 나중에 "PG랑 우리 DB랑 실제로
 * 얼마나 어긋났는지" 알아보려고 별도 배치를 돌 필요가 없다 - 이미 필요한 정보가 이 시점에 다 있다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DepositReconciliationLogRecorder {

    private final DepositReconciliationLogRepository repository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final TransactionTemplate transactionTemplate;

    public void record(Long depositId, ReconciliationFailureType failureType, String providerTxId, Exception cause) {
        log.error("수동 확인 필요. depositId={}, failureType={}, providerTxId={}",
                depositId, failureType, providerTxId, cause);

        String pgSnapshot = inquireSnapshot(providerTxId, cause);

        // 호출부(DepositApplicationService)가 이미 예외를 던지는 흐름이라, 이 저장까지 같은
        // 트랜잭션에 묶이면 롤백에 같이 휩쓸린다 - 그래서 별도 트랜잭션으로 분리해서 저장한다.
        transactionTemplate.executeWithoutResult(status -> repository.save(
                DepositReconciliationLog.open(depositId, failureType, providerTxId, cause.getMessage(), pgSnapshot)
        ));
    }

    private String inquireSnapshot(String providerTxId, Exception cause) {
        if (cause instanceof ResourceAccessException) {
            return "PG와 연결 자체가 불가능한 상태로 판단되어 조회를 생략함";
        }
        try {
            PgInquiryResult inquiry = paymentGatewayClient.inquire(providerTxId);
            return "status=%s, orderId=%s, totalAmount=%s, balanceAmount=%s".formatted(
                    inquiry.status(), inquiry.orderId(), inquiry.totalAmount(), inquiry.balanceAmount());
        } catch (Exception inquiryFailure) {
            return "PG 상태 조회마저 실패: " + inquiryFailure.getMessage();
        }
    }
}