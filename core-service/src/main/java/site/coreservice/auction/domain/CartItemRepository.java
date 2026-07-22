package site.coreservice.auction.domain;

import java.util.List;

public interface CartItemRepository {

    List<CartItem> findAllByMemberId(Long memberId);

}
