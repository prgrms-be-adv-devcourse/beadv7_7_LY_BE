package site.coreservice.auction.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ModifyAuctionCommand(
        Long auctionId,
        Long productId,
        String itemCondition,
        String itemDescription,
        List<String> itemImages,
        BigDecimal startPrice,
        BigDecimal shippingFee,
        BigDecimal bidUnit,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean extensionEnabled,
        Integer extensionTime
) {
}
