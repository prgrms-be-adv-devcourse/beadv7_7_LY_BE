package site.auctionservice.application.port;

import site.auctionservice.application.port.dto.WalletHoldInfo;
import site.auctionservice.domain.Money;

public interface WalletPort {
    WalletHoldInfo hold(Long auctionId, Long memberId, Money amount);

    void rollback(Long holdId, Long auctionId, Long memberId, Money amount);
}
