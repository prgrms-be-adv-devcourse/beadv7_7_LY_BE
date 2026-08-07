package site.auctionservice.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.auctionservice.domain.CartItem;
import site.auctionservice.domain.CartItemRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryImpl implements CartItemRepository {

    private final CartItemJpaRepository cartItemJpaRepository;

    @Override
    public CartItem save(final CartItem cartItem) {
        return cartItemJpaRepository.save(cartItem);
    }

    @Override
    public List<CartItem> findAllByMemberId(final Long memberId) {
        return cartItemJpaRepository.findAllByMemberId(memberId);
    }

    @Override
    public boolean existsByMemberIdAndAuctionId(final Long memberId, final Long auctionId) {
        return cartItemJpaRepository.existsByMemberIdAndAuctionId(memberId, auctionId);
    }

    @Override
    public long countByMemberId(final Long memberId) {
        return cartItemJpaRepository.countByMemberId(memberId);
    }

    @Override
    public void deleteByMemberIdAndAuctionId(final Long memberId, final Long auctionId) {
        cartItemJpaRepository.deleteByMemberIdAndAuctionId(memberId, auctionId);
    }
}
