package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.auction.domain.CartItem;
import site.coreservice.auction.domain.CartItemRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryImpl implements CartItemRepository {

    private final CartItemJpaRepository cartItemJpaRepository;

    @Override
    public List<CartItem> findAllByMemberId(final Long memberId) {
        return cartItemJpaRepository.findAllByMemberId(memberId);
    }
}
