package site.coreservice.product.domain;

import java.util.List;

public interface WishlistItemRepository {

    List<WishlistItem> findAllByMemberId(Long memberId);

}
