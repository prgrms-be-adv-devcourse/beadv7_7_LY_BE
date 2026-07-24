package site.coreservice.pointwallet.hold.presentation.dto;

import java.math.BigDecimal;

public record HoldRequest(Long auctionId, Long memberId, BigDecimal amount) {

    public HoldRequest {
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