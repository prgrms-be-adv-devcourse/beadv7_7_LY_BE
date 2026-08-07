package site.pointwalletservice.hold.application;
import site.pointwalletservice.shared.Money;

/**
 * @param releasedHoldId 이 경매에 기존 활성 홀드가 없었으면 null
 */
public record HoldResult(Long holdId, Long releasedHoldId, Money balanceAfter) {}