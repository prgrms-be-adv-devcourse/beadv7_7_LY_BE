package site.pointwalletservice.hold.presentation.dto;

import java.math.BigDecimal;

/**
 * hold() 호출 때 보냈던 값을 그대로 실어 보내는 최소 검증용 요청 - holdId(PathVariable)만 믿지 않고,
 * 서버가 원장·Hold로 재구성한 실제 값과 대조한다. 필드 구성은 HoldRequest와 동일하게 맞췄다.
 */
public record HoldRollbackRequest(Long auctionId, Long memberId, BigDecimal amount) {

    public HoldRollbackRequest {
        if (auctionId == null) {
            throw new IllegalArgumentException("경매 ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("입찰 금액은 필수입니다.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("입찰 금액은 0보다 커야 합니다.");
        }
    }
}