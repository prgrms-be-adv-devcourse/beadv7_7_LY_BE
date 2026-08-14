// wallet/application/WithdrawFeeEarnedEventHandler.java
package site.pointwalletservice.wallet.application;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionRepository;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.shared.PlatformAccount;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

/**
 * WithdrawFeeEarnedEvent를 받아 플랫폼 계정에 수수료를 적립한다. 모든 인출의 수수료가 여기로
 * 모이므로, Kafka 파티션을 1개로 두거나 발행 시 항상 같은 키를 쓰면 이 핸들러가 한 번에 하나씩만
 * 순차 처리하게 되어 플랫폼 계정 락 경합 자체가 생기지 않는다(WithdrawApplicationService에서
 * withdrawId를 파티션 키로 쓰는 것과 별개로, 컨슈머 자체를 단일 파티션/단일 스레드로 두는 건
 * 인프라 설정 쪽 확인이 필요하다).
 * <p>
 * Kafka는 at-least-once라 같은 이벤트가 중복 전달될 수 있다 - withdrawId+FEE_INCOME 조합의
 * PointTransaction이 이미 있으면 중복 적립을 막기 위해 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawFeeEarnedEventHandler {

    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional
    public void handle(WithdrawFeeEarnedEvent event) {
        if (pointTransactionRepository.existsByRelatedIdAndType(event.withdrawId(), PointTransactionType.FEE_INCOME)) {
            log.info("이미 처리된 인출 수수료 이벤트 — 중복 전달로 판단해 건너뜀. withdrawId={}", event.withdrawId());
            return;
        }

        Money feeAmount = Money.of(event.feeAmount());
        WalletBalanceResult result = walletService.charge(PlatformAccount.PLATFORM_USER_ID, feeAmount);
        pointTransactionService.record(
                result.walletId(), PointTransactionType.FEE_INCOME, feeAmount, result.balanceAfter(), event.withdrawId()
        );
    }
}