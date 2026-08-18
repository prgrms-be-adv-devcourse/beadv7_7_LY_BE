// outbox/application/OutboxAdminService.java
package site.pointwalletservice.outbox.application;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import site.pointwalletservice.outbox.exception.OutboxErrorCode;
import site.pointwalletservice.outbox.exception.OutboxException;

/**
 * DEAD로 넘어간 아웃박스 이벤트를 사람이 조회/재시도하는 최소 관리자 기능. OutboxRelay의 자동
 * 재시도(최대 MAX_RETRY_COUNT회)가 다 소진된 뒤에도, 실패 원인이 일시적이었다면(예: Kafka 장애가
 * 나중에 복구됨) 사람이 확인하고 다시 태울 수 있어야 한다 - 이게 "자동 재시도 실패 = 영구 포기"가
 * 아니라 "사람이 판단할 차례로 넘어감"이라는 뜻이다.
 */
@Service
@RequiredArgsConstructor
public class OutboxAdminService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(readOnly = true)
    public List<OutboxEvent> findDeadEvents() {
        return outboxEventRepository.findDeadNewestFirst();
    }

    @Transactional
    public void retryManually(Long id) {
        OutboxEvent outboxEvent = outboxEventRepository.findById(id)
                .orElseThrow(() -> new OutboxException(OutboxErrorCode.OUTBOX_EVENT_NOT_FOUND));
        outboxEvent.retryManually();
        outboxEventRepository.save(outboxEvent);
    }
}