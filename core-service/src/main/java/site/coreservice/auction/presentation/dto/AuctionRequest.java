package site.coreservice.auction.presentation.dto;

import lombok.NonNull;
import site.coreservice.auction.application.dto.CreateAuctionCommand;
import site.coreservice.auction.application.dto.ModifyAuctionCommand;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionRequest(
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
    public CreateAuctionCommand toCreateCommand() {
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

    public ModifyAuctionCommand toModifyCommand(Long id) {
        return new ModifyAuctionCommand(
                id,
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
