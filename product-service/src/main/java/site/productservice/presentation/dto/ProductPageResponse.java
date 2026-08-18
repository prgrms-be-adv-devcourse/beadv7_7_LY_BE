package site.productservice.presentation.dto;

import java.util.List;
import site.productservice.application.dto.ProductPageResult;

public record ProductPageResponse(List<ProductSnapshotResponse> items, Long nextCursor,
                                  boolean hasNext) {

    public static ProductPageResponse from(ProductPageResult result) {
        return new ProductPageResponse(
            result.items().stream().map(ProductSnapshotResponse::from).toList(),
            result.nextCursor(),
            result.hasNext()
        );
    }
}
