package site.auctionservice.application.port.dto;

import java.math.BigDecimal;

public record WalletHoldInfo(Long holdId, Long releasedHoldId, BigDecimal balanceAfter) {
}
