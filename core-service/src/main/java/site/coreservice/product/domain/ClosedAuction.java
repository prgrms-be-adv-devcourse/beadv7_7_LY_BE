package site.coreservice.product.domain;

import java.time.LocalDateTime;

/**
 * 시세 기록에 필요한 마감 경매 정보. 경매 도메인의 응답 스키마가 아니라
 * "상품 도메인이 필요로 하는 필드"의 선언이다 — 조회 경로(스텁/직접 호출/HTTP)가 바뀌어도 이 형태는 유지된다.
 */
public record ClosedAuction(Long auctionId, Long productId, MediaCondition mediaCondition, Long finalPrice,
        Integer bidCount, LocalDateTime closedAt, String status) {

    /** 여기서 "마감"은 낙찰로 거래가 성립한 상태만 뜻한다 — 경매 도메인의 ENDED_WON. 유찰·취소는 시세 대상이 아니다. */
    public boolean isClosed() {
        return "ENDED_WON".equals(status);
    }
}
