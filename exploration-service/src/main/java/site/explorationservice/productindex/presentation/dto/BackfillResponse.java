package site.explorationservice.productindex.presentation.dto;

import java.util.List;
import site.explorationservice.productindex.application.dto.BackfillResult;

public record BackfillResponse(int totalIndexed, Long nextCursor, List<Long> failedProductIds) {

    public static BackfillResponse from(final BackfillResult result) {
        return new BackfillResponse(result.totalIndexed(), result.nextCursor(),
            result.failedProductIds());
    }
}
