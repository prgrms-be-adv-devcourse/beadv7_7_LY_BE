package site.pointwalletservice.hold.application;
import site.pointwalletservice.shared.Money;

public interface HoldService {

    /**
     * 경매(auctionId)에 새 최고 입찰(userId, amount)을 홀드한다.
     * 이미 이 경매에 활성 홀드가 있으면(다른 유저의 이전 최고 입찰) 그 홀드를 먼저 해제하고 새로 홀드한다.
     */
    HoldResult hold(Long auctionId, Long userId, Money amount);

    /** 주문 취소 등으로 낙찰이 무산됐을 때 — 홀드를 지갑에 환원하고 소멸시킨다. */
    void release(Long auctionId);

    /** 거래 확정(정산 확정)됐을 때 — 이미 hold() 시점에 지갑에서 빠져나간 돈이라, 지갑을 추가로 건드리지 않고 홀드 레코드만 소멸시킨다. */
    void consume(Long auctionId);
}