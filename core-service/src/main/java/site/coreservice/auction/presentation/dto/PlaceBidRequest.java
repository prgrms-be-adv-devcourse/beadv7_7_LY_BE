package site.coreservice.auction.presentation.dto;

import lombok.NonNull;
import site.coreservice.auction.application.dto.PlaceBidCommand;


import java.math.BigDecimal;

public record PlaceBidRequest(@NonNull BigDecimal bidAmount)
{
    public PlaceBidCommand toCommand(Long auctionId, Long bidderId) {
        return new PlaceBidCommand(auctionId, bidderId, bidAmount);
    }
}
