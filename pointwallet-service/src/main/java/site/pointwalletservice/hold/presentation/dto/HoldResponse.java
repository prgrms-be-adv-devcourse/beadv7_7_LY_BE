package site.pointwalletservice.hold.presentation.dto;

import java.math.BigDecimal;

/**
 * @param releasedHoldId 이 경매에 기존 활성 홀드가 없었으면 null
 */
public record HoldResponse(Long holdId, Long releasedHoldId, BigDecimal balanceAfter) {}