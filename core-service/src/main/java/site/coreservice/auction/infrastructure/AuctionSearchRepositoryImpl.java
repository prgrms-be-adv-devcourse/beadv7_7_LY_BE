package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

@Component
@RequiredArgsConstructor
public class AuctionSearchRepositoryImpl implements AuctionSearchViewRepository {

    private final AuctionSearchViewJpaRepository searchViewJpaRepository;

    @Override
    public void save(Auction auction, ProductSnapshot product, String sellerNickname) {
        searchViewJpaRepository.save(AuctionSearchView.from(auction, product, sellerNickname));
    }

    @Override
    public void updateFromAuction(Auction auction, ProductSnapshot product) {
        AuctionSearchView view = searchViewJpaRepository.findById(auction.getId()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND));
        view.updateFromAuction(auction, product);
    }

    @Override
    public void updateStatus(Auction auction) {
        AuctionSearchView view = searchViewJpaRepository.findById(auction.getId()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND));
        view.updateStatus(auction.getStatus());
    }

    @Override
    public void deleteById(Long auctionId) {
        searchViewJpaRepository.deleteById(auctionId);
    }

}
