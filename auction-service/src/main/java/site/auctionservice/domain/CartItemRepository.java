package site.auctionservice.domain;

import java.util.List;

public interface CartItemRepository {

    CartItem save(CartItem cartItem);

    List<CartItem> findAllByMemberId(Long memberId);

    boolean existsByMemberIdAndAuctionId(Long memberId, Long auctionId);

    long countByMemberId(Long memberId);

    void deleteByMemberIdAndAuctionId(Long memberId, Long auctionId);

}
