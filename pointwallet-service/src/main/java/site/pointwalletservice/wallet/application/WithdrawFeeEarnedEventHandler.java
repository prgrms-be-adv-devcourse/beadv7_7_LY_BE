package site.pointwalletservice.wallet.application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.ledger.application.PointTransactionService;
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
 * <p>
 * 주의: 아래 existsForRelatedId 체크는 check-then-act라 두 스레드가 동시에 같은 이벤트를
 * 처리하면(예: 파티션 설정이 예상과 다르거나 리밸런싱 순간 등) 이 체크만으로는 중복 적립을 완전히
 * 막지 못한다. 실제 안전망은 point_transaction(related_id, type) 유니크 제약이다 - record()가
 * 그 제약을 위반하면 DataIntegrityViolationException이 터지고, 이 메서드가 @Transactional이라
 * 방금 이 트랜잭션에서 한 charge()까지 통째로 롤백된다. 그 예외를 "중복 전달"로 해석해서 정상
 * 종료시키는 건 이 클래스가 아니라 호출부(WithdrawFeeEarnedEventListener)의 책임이다 - 트랜잭션
 * 프록시 바깥에서 잡아야 롤백된 상태를 조용히 넘길 수 있기 때문이다.
 * <p>
 * ledger 도메인엔 PointTransactionService(포트)로만 접근한다 - PointTransactionRepository를
 * 직접 주입받지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawFeeEarnedEventHandler {

    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;

    @Transactional
    public void handle(WithdrawFeeEarnedEvent event) {
        if (pointTransactionService.existsForRelatedId(event.withdrawId(), PointTransactionType.FEE_INCOME)) {
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