package site.coreservice.auction.application.port;

import site.coreservice.auction.application.port.dto.WalletHoldInfo;
import site.coreservice.auction.domain.Money;

public interface WalletPort {
    WalletHoldInfo hold(Long auctionId, Long memberId, Money amount);
}
