package site.productservice.application.port;

import java.util.List;
import java.util.Map;

/**
 * 상품별 진행 중 경매 수 조회 창구. 반환이 남의 도메인 조각(상품 id → 건수 숫자)이라 도메인이 아닌
 * application/port에 둔다 (ADR-010 — 행위 있는 VO를 반환하는 AuctionSnapshotPort가 domain에 있는 것과 대비).
 * 구현체는 infrastructure/client의 AuctionOpenCountHttpClient.
 * 호출 실패 시 예외를 그대로 던진다 — 목록을 살릴지 죽일지는 호출자가 정한다.
 */
public interface AuctionOpenCountPort {

    Map<Long, Long> findOpenAuctionCounts(List<Long> productIds);
}
