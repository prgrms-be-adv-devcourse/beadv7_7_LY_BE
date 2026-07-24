package site.coreservice.auction.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.auction.application.dto.AuctionResult;
import site.coreservice.auction.application.dto.CreateAuctionCommand;
import site.coreservice.auction.application.dto.ModifyAuctionCommand;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.MemberPort;
import site.coreservice.auction.application.port.ProductPort;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.*;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final MemberPort memberPort;
    private final ProductPort productPort;
    private final AuctionSearchViewRepository searchViewRepository;

    @Transactional
    public AuctionResult createAuction(CreateAuctionCommand command, Long sellerId) {
        String sellerNickname = memberPort.getNickname(sellerId);
        ProductSnapshot productSnapshot = productPort.getProduct(command.productId());
        Auction auction = Auction.register(
                sellerId, command.productId(),
                ItemInfo.of(ItemCondition.from(command.itemCondition()), command.itemDescription(), command.itemImages()),
                Pricing.of(Money.from(command.startPrice()), Money.from(command.bidUnit()), Money.from(command.shippingFee())),
                AuctionSchedule.of(Period.of(command.startAt(), command.endAt()), command.extensionEnabled(), command.extensionTime())
        );
        auctionRepository.save(auction);
        searchViewRepository.save(auction, productSnapshot, sellerNickname);

        return AuctionResult.from(auction);
    }

    @Transactional
    public AuctionResult modifyAuction(ModifyAuctionCommand command, Long sellerId) {
        Auction auction = auctionRepository.findById(command.auctionId()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        boolean productChanged = !auction.getProductId().equals(command.productId());

        auction.modify(sellerId, command.productId(),
                ItemInfo.of(ItemCondition.from(command.itemCondition()), command.itemDescription(), command.itemImages()),
                Pricing.of(Money.from(command.startPrice()), Money.from(command.bidUnit()), Money.from(command.shippingFee())),
                AuctionSchedule.of(Period.of(command.startAt(), command.endAt()), command.extensionEnabled(), command.extensionTime()),
                LocalDateTime.now()
        );

        ProductSnapshot product = productChanged ? productPort.getProduct(command.productId()) : null;
        searchViewRepository.updateFromAuction(auction, product);
        return AuctionResult.from(auction);
    }

    @Transactional
    public void deleteAuction(Long auctionId, Long sellerId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        auction.cancel(sellerId, LocalDateTime.now());
        searchViewRepository.deleteById(auctionId);
    }
}
