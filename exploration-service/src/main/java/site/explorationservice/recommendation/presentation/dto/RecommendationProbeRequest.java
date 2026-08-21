package site.explorationservice.recommendation.presentation.dto;

import java.util.List;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;

public record RecommendationProbeRequest(
    List<WishlistProduct> products,
    Integer size,
    Boolean useLlm,
    AxisWeights weights
) {

    public List<Long> productIds() {
        return products.stream().map(WishlistProduct::productId).toList();
    }

    public int sizeOrDefault() {
        return size == null ? 0 : size;
    }

    public boolean useLlmOrDefault() {
        return useLlm != null && useLlm;
    }
}
