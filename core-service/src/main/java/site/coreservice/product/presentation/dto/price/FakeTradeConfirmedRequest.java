package site.coreservice.product.presentation.dto.price;

import java.time.LocalDateTime;

public record FakeTradeConfirmedRequest(Long auctionId, LocalDateTime confirmedAt) {
}
