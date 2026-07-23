package site.coreservice.auction.presentation.dto;

import lombok.NonNull;
import site.coreservice.auction.application.dto.CreateAuctionCommand;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionCreateRequest(
        @NonNull Long productId,
        @NonNull String itemCondition,
        String itemDescription,
        @NonNull List<String> itemImages,
        @NonNull BigDecimal startPrice,
        @NonNull BigDecimal shippingFee,
        @NonNull BigDecimal bidUnit,
        @NonNull LocalDateTime startAt,
        @NonNull LocalDateTime endAt,
        @NonNull Boolean extensionEnabled,
        Integer extensionTime
) {
    public CreateAuctionCommand toCommand() {
        return new CreateAuctionCommand(
                productId,
                itemCondition,
                itemDescription,
                itemImages,
                startPrice,
                shippingFee,
                bidUnit,
                startAt,
                endAt,
                extensionEnabled,
                extensionTime
        );
    }
}
