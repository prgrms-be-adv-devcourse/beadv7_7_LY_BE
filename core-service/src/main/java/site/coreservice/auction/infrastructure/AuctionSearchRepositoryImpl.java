package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.exception.AuctionSearchViewNotFoundException;

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
        AuctionSearchView view = searchViewJpaRepository.findById(auction.getId()).orElseThrow(AuctionSearchViewNotFoundException::new);
        view.updateFromAuction(auction, product);
    }

    @Override
    public void deleteById(Long auctionId) {
        searchViewJpaRepository.deleteById(auctionId);
    }

}
