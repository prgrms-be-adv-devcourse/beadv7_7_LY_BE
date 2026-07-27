package site.coreservice.product.domain;

import java.util.List;

public interface WishlistItemRepository {

    WishlistItem save(WishlistItem wishlistItem);

    List<WishlistItem> findAllByMemberId(Long memberId, Long cursor, int limit);

    void deleteByMemberIdAndProductId(Long memberId, Long productId);

}
