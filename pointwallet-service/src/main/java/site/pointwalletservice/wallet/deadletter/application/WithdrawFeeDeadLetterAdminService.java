package site.pointwalletservice.wallet.deadletter.application;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.wallet.application.WithdrawFeeEarnedEventHandler;
import site.pointwalletservice.wallet.deadletter.domain.DeadLetterStatus;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetter;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetterRepository;
import site.pointwalletservice.wallet.exception.WithdrawFeeDeadLetterNotFoundException;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

/**
 * 관리자가 수동으로 확인/재처리하는 최소 기능 - 목록 조회, 재처리(정상 처리 재시도), 그리고
 * (재처리가 의미 없거나 이미 다른 방식으로 정리된 경우를 위한) 단순 확인 처리 세 가지다.
 * 권한/역할 체계는 DepositReconciliationAdminService와 동일하게 아직 여기 없음 - 인증은
 * 상위(게이트웨이 등)에서 내부 API 자체를 막는 걸 전제로 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawFeeDeadLetterAdminService {

    private final WithdrawFeeDeadLetterRepository repository;
    private final WithdrawFeeEarnedEventHandler withdrawFeeEarnedEventHandler;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public List<WithdrawFeeDeadLetter> findByStatus(DeadLetterStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * 원래 이벤트 처리(WithdrawFeeEarnedEventHandler.handle())를 그대로 재호출한다.
     * 중복 적립 걱정 없이 재시도할 수 있는 건 이 핸들러가 이미 point_transaction(related_id, type)
     * 유니크 제약으로 멱등 처리하기 때문이다(WithdrawFeeEarnedEventHandler 클래스 주석 참고) -
     * 재처리가 성공하든, 이미 처리돼 있어 건너뛰든 결과적으로 안전하다.
     * <p>
     * 이 메서드 자체는 @Transactional이 아니다 - handle()이 자기 트랜잭션을 스스로 소유해야
     * 유니크 제약 위반 시 그 트랜잭션만 온전히 롤백되고, 이 메서드를 감싼 트랜잭션이 함께
     * rollback-only로 오염되지 않는다. 만약 이 메서드에 @Transactional을 걸어 handle()이
     * 그 트랜잭션에 참여(REQUIRED)하게 만들면, handle() 내부에서 유니크 제약 위반이 나는 순간
     * (PointTransaction이 IDENTITY 전략이라 save() 시점에 즉시 INSERT되므로 flush를 기다리지
     * 않고 바로 터진다) handle()의 트랜잭션 프록시가 "자신은 소유자가 아니므로" 실제 롤백 대신
     * 트랜잭션을 rollback-only로만 마킹하고 예외를 다시 던진다. 그러면 여기서 캐치해서
     * deadLetter.resolve()까지 정상 실행해도, 이 메서드의 트랜잭션이 커밋을 시도하는 순간
     * rollback-only 마킹을 발견하고 UnexpectedRollbackException을 던지며 resolve() 변경사항까지
     * 통째로 날아간다 - WithdrawFeeEarnedEventListener가 자기 트랜잭션 없이 handle()을 호출해서
     * 이 문제를 피하는 것과 동일한 이유로, 여기서도 트랜잭션 경계를 handle() 안으로 넘겨준다.
     */
    public void reprocess(Long id) {
        WithdrawFeeDeadLetter deadLetter = repository.findById(id)
                .orElseThrow(WithdrawFeeDeadLetterNotFoundException::new);
        Long withdrawId = deadLetter.getWithdrawId();

        try {
            withdrawFeeEarnedEventHandler.handle(
                    new WithdrawFeeEarnedEvent(withdrawId, deadLetter.getFeeAmount()));
            markResolved(id, "관리자 재처리 성공");
            log.info("인출 수수료 DLT 재처리 성공: withdrawId={}", withdrawId);
        } catch (DataIntegrityViolationException e) {
            // 원본 리스너의 재시도가 이 재처리와 거의 동시에 성공한 경우 - handle()이 자기
            // 트랜잭션을 이미 온전히 롤백했으므로, 여기서부터는 깨끗한 상태에서 새 트랜잭션으로
            // 확인 처리만 남기면 된다.
            markResolved(id, "이미 처리된 이벤트로 확인됨(원본 리스너가 동시에 처리 완료) - 재처리 없이 확인 처리");
            log.warn("재처리 중 유니크 제약 위반 - 이미 처리된 것으로 간주. withdrawId={}", withdrawId, e);
        }
    }

    /** resolve 반영을 별도의 새 트랜잭션으로 커밋한다 - reprocess() 자체가 트랜잭션이 아니므로, 여기서 새로 열어야 한다. */
    private void markResolved(Long id, String note) {
        transactionTemplate.executeWithoutResult(status -> {
            WithdrawFeeDeadLetter deadLetter = repository.findById(id)
                    .orElseThrow(WithdrawFeeDeadLetterNotFoundException::new);
            deadLetter.resolve(note);
            repository.save(deadLetter);
        });
    }

    /** 재처리 없이 사유만 남기고 확인 처리하는 경로(예: 재처리해도 의미 없다고 판단한 경우). */
    @Transactional
    public void resolve(Long id, String note) {
        WithdrawFeeDeadLetter deadLetter = repository.findById(id)
                .orElseThrow(WithdrawFeeDeadLetterNotFoundException::new);
        deadLetter.resolve(note);
    }
}