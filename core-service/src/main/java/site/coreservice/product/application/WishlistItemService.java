package site.coreservice.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.product.domain.WishlistItem;
import site.coreservice.product.domain.WishlistItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistItemService {

    private final WishlistItemRepository wishlistItemRepository;

    @Transactional
    public WishlistItem add(final Long memberId, final Long productId) {
        return wishlistItemRepository.save(new WishlistItem(memberId, productId));
    }

    @Transactional
    public void remove(final Long memberId, final Long productId) {
        wishlistItemRepository.deleteByMemberIdAndProductId(memberId, productId);
    }

    @Transactional(readOnly = true)
    public List<WishlistItem> findAll(final Long memberId) {
        return wishlistItemRepository.findAllByMemberId(memberId);
    }
}
