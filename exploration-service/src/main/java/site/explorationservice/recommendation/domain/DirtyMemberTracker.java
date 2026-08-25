package site.explorationservice.recommendation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * "위시리스트가 바뀌었다"는 신호를 모았다가, 변경이 잠잠해진 뒤(디바운스) 재계산 대상만 골라낸다.
 */
public interface DirtyMemberTracker {

    /**
     * 위시리스트 변경 이벤트를 받을 때마다 호출 — 무조건 덮어쓴다. 컨슈머가 이벤트를 받은 시각이 아니라 실제 위시리스트가 바뀐 시각을 기록
     */
    void markDirty(Long memberId, Instant occurredAt);

    /**
     * 디바운스 윈도우를 지난 멤버들을 원자적으로 "처리 중"으로 표시하고 돌려준다. 이미 처리 중인 멤버는 다시 반환하지 않는다
     */
    List<DueMember> claimDue(Duration debounceWindow);

    /**
     * 재계산 성공 시 호출. {@code observedDirtySince}와 현재 dirty 값이 같을 때만(그 사이 새 변경이 없었을 때만) dirty 상태를 정리한다.
     * 다르면 그대로 남겨 다음 클레임 대상이 되게 한다. "처리 중" 표시는 성공 여부와 무관하게 항상 해제한다.
     */
    void complete(Long memberId, Instant observedDirtySince);

    /**
     * 실패시 호출, "처리 중" 표시만 해제한다.
     */
    void release(Long memberId);
}
