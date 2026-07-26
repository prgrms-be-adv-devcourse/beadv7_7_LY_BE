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

        final List<WishlistItem> result = wishlistItemRepository.findAllByMemberId(1L, null, 10);

        assertThat(result)
            .extracting(WishlistItem::getProductId)
            .containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void findAllByMemberId_빈_리스트_반환() {
        final List<WishlistItem> result = wishlistItemRepository.findAllByMemberId(999L, null, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByMemberId는_최근_담은_순으로_반환한다() {
        final WishlistItem first = wishlistItemJpaRepository.save(new WishlistItem(1L, 100L));
        final WishlistItem second = wishlistItemJpaRepository.save(new WishlistItem(1L, 101L));
        final WishlistItem third = wishlistItemJpaRepository.save(new WishlistItem(1L, 102L));

        final List<WishlistItem> result = wishlistItemRepository.findAllByMemberId(1L, null, 10);

        assertThat(result).extracting(WishlistItem::getId)
            .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    void findAllByMemberId는_cursor보다_작은_id만_반환한다() {
        final WishlistItem first = wishlistItemJpaRepository.save(new WishlistItem(1L, 100L));
        final WishlistItem second = wishlistItemJpaRepository.save(new WishlistItem(1L, 101L));
        final WishlistItem third = wishlistItemJpaRepository.save(new WishlistItem(1L, 102L));

        final List<WishlistItem> result = wishlistItemRepository.findAllByMemberId(1L, third.getId(), 10);

        assertThat(result).extracting(WishlistItem::getId)
            .containsExactly(second.getId(), first.getId());
    }

    @Test
    void findAllByMemberId는_limit개까지만_반환한다() {
        wishlistItemJpaRepository.save(new WishlistItem(1L, 100L));
        wishlistItemJpaRepository.save(new WishlistItem(1L, 101L));
        wishlistItemJpaRepository.save(new WishlistItem(1L, 102L));

        final List<WishlistItem> result = wishlistItemRepository.findAllByMemberId(1L, null, 2);

        assertThat(result).hasSize(2);
    }
}
