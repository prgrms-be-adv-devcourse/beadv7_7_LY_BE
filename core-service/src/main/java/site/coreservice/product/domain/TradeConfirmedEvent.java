package site.coreservice.product.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import site.common.event.Event;

/**
 * 거래가 확정되었음을 알리는 이벤트. 실제 발행자는 주문(06)이고 payload는 경매 id와 확정시각뿐 —
 * 나머지 정보는 수신 측이 경매 조회로 채운다. 세미 기간엔 local 전용 가짜 발행기가 대신 발행한다.
 */
@Getter
public class TradeConfirmedEvent extends Event {
    private final Long auctionId;
    private final LocalDateTime confirmedAt;

    public TradeConfirmedEvent(Long auctionId, LocalDateTime confirmedAt) {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId는 필수입니다");
        }
        if (confirmedAt == null) {
            throw new IllegalArgumentException("confirmedAt은 필수입니다");
        }
        this.auctionId = auctionId;
        this.confirmedAt = confirmedAt;
    }

    @Override
    public String getEventType() {
        return "trade.confirmed";
    }
}
