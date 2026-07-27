package site.coreservice.auction.infrastructure.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import site.coreservice.auction.application.port.WalletPort;
import site.coreservice.auction.application.port.dto.WalletHoldInfo;
import site.coreservice.auction.domain.Money;

import java.math.BigDecimal;

@Component
@Profile("local")
public class MockWalletClient implements WalletPort {
    @Override
    public WalletHoldInfo hold(Long auctionId, Long memberId, Money amount) {
        return new WalletHoldInfo(1L, null, BigDecimal.valueOf(1_000_000));
    }
}
