package site.explorationservice.productindex.application.dto;

import java.util.List;

/**
 * nextCursor는 이번 호출이 처리하지 못하고 남긴 지점이다 — null이면 남은 상품 없이 끝까지 처리했다는 뜻이고, 값이 있으면 다음 호출의 startCursor에
 * 그대로 넣어 이어서 처리한다.
 */
public record BackfillResult(int totalIndexed, Long nextCursor, List<Long> failedProductIds) {

}
