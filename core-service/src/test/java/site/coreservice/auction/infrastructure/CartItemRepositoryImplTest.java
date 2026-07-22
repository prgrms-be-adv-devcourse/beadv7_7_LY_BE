package site.coreservice.auction.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.coreservice.auction.domain.CartItem;
import site.coreservice.auction.domain.CartItemRepository;
import site.coreservice.support.RepositoryTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@Import(CartItemRepositoryImpl.class)
class CartItemRepositoryImplTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartItemJpaRepository cartItemJpaRepository;

    @Test
    void findAllByMemberId는_해당_회원의_장바구니_아이템만_반환한다() {
        cartItemJpaRepository.save(new CartItem(1L, 100L));
        cartItemJpaRepository.save(new CartItem(1L, 101L));
        cartItemJpaRepository.save(new CartItem(2L, 200L));

        final List<CartItem> result = cartItemRepository.findAllByMemberId(1L);

        assertThat(result)
            .extracting(CartItem::getProductId)
            .containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void findAllByMemberId_빈_리스트_반환() {
        final List<CartItem> result = cartItemRepository.findAllByMemberId(999L);

        assertThat(result).isEmpty();
    }
}
