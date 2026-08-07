package site.fulfillmentservice.order.domain;

import java.util.List;

public record OrderSearchPage(List<Order> content, long totalElements) {
}
