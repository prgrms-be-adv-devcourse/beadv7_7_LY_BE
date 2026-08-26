package site.pointwalletservice.wallet.deadletter.application;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
     * DataIntegrityViolationException(유니크 제약 위반)은 원본 리스너의 재시도가 이 재처리와
     * 거의 동시에 성공해버린 경우에 날 수 있다 - WithdrawFeeEarnedEventListener가 같은 예외를
     * "중복 전달로 판단해 정상 종료"시키는 것과 동일하게, 여기서도 실패가 아니라 이미 처리된
     * 것으로 간주해 확인 처리한다.
     */
    @Transactional
    public void reprocess(Long id) {
        WithdrawFeeDeadLetter deadLetter = repository.findById(id)
                .orElseThrow(WithdrawFeeDeadLetterNotFoundException::new);

        try {
            withdrawFeeEarnedEventHandler.handle(
                    new WithdrawFeeEarnedEvent(deadLetter.getWithdrawId(), deadLetter.getFeeAmount()));
            deadLetter.resolve("관리자 재처리 성공");
            log.info("인출 수수료 DLT 재처리 성공: withdrawId={}", deadLetter.getWithdrawId());
        } catch (DataIntegrityViolationException e) {
            deadLetter.resolve("이미 처리된 이벤트로 확인됨(원본 리스너가 동시에 처리 완료) - 재처리 없이 확인 처리");
            log.warn("재처리 중 유니크 제약 위반 - 이미 처리된 것으로 간주. withdrawId={}",
                    deadLetter.getWithdrawId(), e);
        }
    }

    /** 재처리 없이 사유만 남기고 확인 처리하는 경로(예: 재처리해도 의미 없다고 판단한 경우). */
    @Transactional
    public void resolve(Long id, String note) {
        WithdrawFeeDeadLetter deadLetter = repository.findById(id)
                .orElseThrow(WithdrawFeeDeadLetterNotFoundException::new);
        deadLetter.resolve(note);
    }
}