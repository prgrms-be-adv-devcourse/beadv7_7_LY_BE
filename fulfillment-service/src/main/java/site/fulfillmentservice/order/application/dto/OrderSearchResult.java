package site.fulfillmentservice.order.application.dto;

import java.util.List;
import site.fulfillmentservice.order.domain.OrderSearchPage;

public record OrderSearchResult(List<OrderSummaryResult> content, int page, int size,
                                 long totalElements, int totalPages, boolean last) {

    public static OrderSearchResult of(OrderSearchPage searchPage, int page, int size) {
        List<OrderSummaryResult> content = searchPage.content().stream()
                .map(OrderSummaryResult::from)
                .toList();
        int totalPages = (int) Math.ceil((double) searchPage.totalElements() / size);
        boolean last = (page + 1) >= totalPages;
        return new OrderSearchResult(content, page, size, searchPage.totalElements(), totalPages, last);
    }
}
