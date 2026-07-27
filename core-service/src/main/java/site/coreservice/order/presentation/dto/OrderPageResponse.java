package site.coreservice.order.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import site.coreservice.order.application.dto.OrderSearchResult;
import site.coreservice.order.application.dto.OrderSummaryResult;
import site.coreservice.order.application.dto.OrderItemSnapshotResult;

public record OrderPageResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static OrderPageResponse from(OrderSearchResult result) {
        List<Item> items = result.content().stream()
                .map(Item::from)
                .toList();
        return new OrderPageResponse(
                items,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.last()
        );
    }

    public record Item(
            Long orderId,
            Long auctionId,
            String status,
            BigDecimal finalBidPrice,
            LocalDateTime confirmationDeadline,
            LocalDateTime completionDeadline,
            Product product
    ) {

        public static Item from(OrderSummaryResult result) {
            return new Item(
                    result.orderId(),
                    result.auctionId(),
                    result.status(),
                    result.finalBidPrice(),
                    result.confirmationDeadline(),
                    result.completionDeadline(),
                    Product.from(result.product())
            );
        }
    }

    public record Product(
            Long productId,
            String albumTitle,
            String artistName,
            String conditionGrade,
            String coverImage
    ) {

        public static Product from(OrderItemSnapshotResult result) {
            return new Product(
                    result.productId(),
                    result.albumTitle(),
                    result.artistName(),
                    result.conditionGrade(),
                    result.coverImage()
            );
        }
    }
}
