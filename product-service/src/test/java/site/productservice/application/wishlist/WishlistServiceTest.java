package site.productservice.application.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.productservice.application.dto.wishlist.WishlistItemPage;
import site.productservice.domain.wishlist.WishlistItem;
import site.productservice.domain.wishlist.WishlistItemRepository;

/**
 * 저장소 접근과 커서 페이징 계산을 검증한다. 상품 정보를 채우거나 비활성 상품을 걸러내는 건 파사드 몫이라 그 검증은 WishlistServiceFacadeTest에 있다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistService")
class WishlistServiceTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    private WishlistService service() {
        return new WishlistService(wishlistItemRepository);
    }

    @Test
    @DisplayName("저장을 리포지토리에 위임한다")
    void add는_리포지토리에_위임한다() {
        final WishlistItem saved = WishlistItem.of(1L, 100L);
        when(wishlistItemRepository.save(any())).thenReturn(saved);

        final WishlistItem result = service().add(1L, 100L);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    @DisplayName("삭제를 리포지토리에 위임한다")
    void remove는_리포지토리에_위임한다() {
        service().remove(1L, 100L);

        verify(wishlistItemRepository).deleteByMemberIdAndProductId(1L, 100L);
    }

    @Test
    @DisplayName("다음 페이지 존재를 알아내려고 size보다 한 건 더 조회한다")
    void findPage는_size보다_한건_더_조회한다() {
        when(wishlistItemRepository.findAllByMemberId(1L, null, 11)).thenReturn(List.of());

        service().findPage(1L, null, 10);

        verify(wishlistItemRepository).findAllByMemberId(1L, null, 11);
    }

    @Test
    @DisplayName("cursor를 그대로 리포지토리에 전달한다")
    void findPage는_cursor를_그대로_전달한다() {
        when(wishlistItemRepository.findAllByMemberId(1L, 50L, 11)).thenReturn(List.of());

        service().findPage(1L, 50L, 10);

        verify(wishlistItemRepository).findAllByMemberId(1L, 50L, 11);
    }

    @Test
    @DisplayName("size보다 많이 조회되면 잘라내고 hasNext와 nextCursor를 설정한다")
    void findPage는_넘치면_hasNext와_nextCursor를_설정한다() {
        final WishlistItem first = itemWithId(100L, 30L);
        final WishlistItem second = itemWithId(101L, 20L);
        final WishlistItem third = itemWithId(102L, 10L);
        when(wishlistItemRepository.findAllByMemberId(1L, null, 3))
            .thenReturn(List.of(first, second, third));

        final WishlistItemPage page = service().findPage(1L, null, 2);

        assertThat(page.items()).containsExactly(first, second);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(20L);
    }

    @Test
    @DisplayName("size 이하로 조회되면 hasNext가 false이고 nextCursor가 없다")
    void findPage는_넘치지_않으면_다음_페이지가_없다() {
        final WishlistItem only = itemWithId(100L, 30L);
        when(wishlistItemRepository.findAllByMemberId(1L, null, 11)).thenReturn(List.of(only));

        final WishlistItemPage page = service().findPage(1L, null, 10);

        assertThat(page.items()).containsExactly(only);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("size가 0 이하면 기본값 20으로 조회한다")
    void findPage는_size가_0이하면_기본값을_쓴다() {
        when(wishlistItemRepository.findAllByMemberId(1L, null, 21)).thenReturn(List.of());

        service().findPage(1L, null, 0);

        verify(wishlistItemRepository).findAllByMemberId(1L, null, 21);
    }

    @Test
    @DisplayName("size가 최대치를 넘으면 100으로 제한한다")
    void findPage는_size가_최대치를_넘으면_제한한다() {
        when(wishlistItemRepository.findAllByMemberId(1L, null, 101)).thenReturn(List.of());

        service().findPage(1L, null, 500);

        verify(wishlistItemRepository).findAllByMemberId(1L, null, 101);
    }

    private WishlistItem itemWithId(final Long productId, final Long id) {
        final WishlistItem item = WishlistItem.of(1L, productId);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
