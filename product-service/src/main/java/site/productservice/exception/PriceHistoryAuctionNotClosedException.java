package site.productservice.exception;

import site.common.exception.BusinessException;

/**
 * 거래확정 이벤트인데 조회한 경매가 마감 상태가 아닐 때. 확정됐다면 마감이어야 하므로 모순이고,
 * 사용자 입력이 아니라 내부 정합성 오류라 49xx 대역(PERR-4902)이다. 리스너 방벽에 삼켜져 로그로만 남는다.
 */
public class PriceHistoryAuctionNotClosedException extends BusinessException {

    public PriceHistoryAuctionNotClosedException(Long auctionId, String status) {
        super(ProductErrorCode.PRICE_HISTORY_AUCTION_NOT_CLOSED,
                "거래확정 이벤트인데 경매가 마감 상태가 아닙니다 — auctionId: " + auctionId + ", status: " + status);
    }
}
