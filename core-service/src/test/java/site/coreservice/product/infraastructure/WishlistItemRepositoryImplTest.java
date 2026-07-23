package site.coreservice.product.infraastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import site.coreservice.product.infrastructure.WishlistItemJpaRepository;
import site.coreservice.product.infrastructure.WishlistItemRepositoryImpl;
import site.coreservice.support.RepositoryTest;
import site.coreservice.product.domain.WishlistItem;
import site.coreservice.product.domain.WishlistItemRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@Import(WishlistItemRepositoryImpl.class)
class WishlistItemRepositoryImplTest {

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private WishlistItemJpaRepository wishlistItemJpaRepository;

    @Test
    void findAllByMemberId는_해당_회원의_위시리스트_아이템만_반환한다() {
        wishlistItemJpaRepository.save(new WishlistItem(1L, 100L));
        wishlistItemJpaRepository.save(new WishlistItem(1L, 101L));
        wishlistItemJpaRepository.save(new WishlistItem(2L, 200L));

        final List<WishlistItem> result = wishlistItemRepository.findAllByMemberId(1L);

        assertThat(result)
            .extracting(WishlistItem::getProductId)
            .containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void findAllByMemberId_빈_리스트_반환() {
        final List<WishlistItem> result = wishlistItemRepository.findAllByMemberId(999L);

        assertThat(result).isEmpty();
    }
}
