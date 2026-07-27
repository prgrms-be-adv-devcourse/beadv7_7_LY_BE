package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import site.coreservice.product.domain.WishlistItem;
import site.coreservice.product.domain.WishlistItemRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WishlistItemRepositoryImpl implements WishlistItemRepository {

    private final WishlistItemJpaRepository wishlistItemJpaRepository;

    @Override
    public WishlistItem save(final WishlistItem wishlistItem) {
        return wishlistItemJpaRepository.save(wishlistItem);
    }

    @Override
    public List<WishlistItem> findAllByMemberId(final Long memberId, final Long cursor,
        final int limit) {
        return wishlistItemJpaRepository.findAllByMemberId(memberId, cursor,
            PageRequest.of(0, limit));
    }

    @Override
    public void deleteByMemberIdAndProductId(final Long memberId, final Long productId) {
        wishlistItemJpaRepository.deleteByMemberIdAndProductId(memberId, productId);
    }
}
