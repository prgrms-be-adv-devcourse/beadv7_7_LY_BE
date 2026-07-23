package site.coreservice.auction.domain;

public enum AuctionStatus {
    SCHEDULED,      // 진행 전
    RUNNING,        // 진행 중
    ENDED_WON,      // 낙찰
    ENDED_FAILED,   // 유착
    CANCELED;       // 취소

    public boolean canTransitTo(AuctionStatus next) {
        return switch (this) {
            case SCHEDULED -> next == RUNNING || next == CANCELED;
            case RUNNING   -> next == ENDED_WON || next == ENDED_FAILED;
            default        -> false;
        };
    }
}
