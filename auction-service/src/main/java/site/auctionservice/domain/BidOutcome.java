package site.auctionservice.domain;

public enum BidOutcome {
    ACTIVE,         // 최고 입찰 상태
    OUTBID,         // 다른 입찰이 밀린 상태
    WON;             // 낙찰

    public boolean canTransitTo(BidOutcome next) {
        return this == ACTIVE && (next == OUTBID || next == WON);
    }
}
