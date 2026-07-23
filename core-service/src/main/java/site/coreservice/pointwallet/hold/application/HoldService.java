package site.coreservice.pointwallet.hold.application;

import site.coreservice.pointwallet.shared.Money;

public interface HoldService {

    /**
     * 경매(auctionId)에 새 최고 입찰(userId, amount)을 홀드한다.
     * 이미 이 경매에 활성 홀드가 있으면(다른 유저의 이전 최고 입찰) 그 홀드를 먼저 해제하고 새로 홀드한다.
     */
    HoldResult hold(Long auctionId, Long userId, Money amount);
}