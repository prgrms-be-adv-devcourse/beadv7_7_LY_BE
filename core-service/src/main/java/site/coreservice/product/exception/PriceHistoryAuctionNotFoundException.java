package site.coreservice.product.exception;

import site.common.exception.BusinessException;

/**
 * 거래확정 이벤트로 시세를 적재하려는데 그 경매를 조회할 수 없을 때. 정상 흐름에선 일어나면 안 되는
 * 내부 정합성 오류라 49xx 대역(PERR-4901)이다. 리스너의 예외 방벽에 삼켜져 로그로만 남는다.
 */
public class PriceHistoryAuctionNotFoundException extends BusinessException {

    public PriceHistoryAuctionNotFoundException(Long auctionId) {
        super(ProductErrorCode.PRICE_HISTORY_AUCTION_NOT_FOUND,
                "시세를 적재할 경매를 찾을 수 없습니다 — auctionId: " + auctionId);
    }
}
