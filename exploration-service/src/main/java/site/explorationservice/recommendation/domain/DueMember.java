package site.explorationservice.recommendation.domain;

import java.time.Instant;

/**
 * 디바운스 임계값을 지나 가중치 재계산 대상이 된 멤버. {@code dirtySince}는 클레임 당시 관찰한 값
 * {@link DirtyMemberTracker#complete}에 그대로 넘겨서, 처리 도중 새 변경이 왔는지(CAS)를 판단하는 데 쓴다.
 */
public record DueMember(Long memberId, Instant dirtySince) {

}
