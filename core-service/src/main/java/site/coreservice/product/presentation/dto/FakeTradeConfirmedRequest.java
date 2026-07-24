package site.coreservice.product.presentation.dto;

import java.time.LocalDateTime;

public record FakeTradeConfirmedRequest(Long auctionId, LocalDateTime confirmedAt) {
}
