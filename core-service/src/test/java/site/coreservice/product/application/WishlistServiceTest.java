package site.coreservice.product.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.product.application.dto.ProductSnapshotResult;
import site.coreservice.product.application.dto.WishlistItemPageResult;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.WishlistItem;
import site.coreservice.product.domain.WishlistItemRepository;
import site.coreservice.product.exception.ProductNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private ProductService productService;

    @Test
    void findPage은_각_아이템에_해당_상품_정보를_채워_반환한다() {
        final WishlistItem wishlistItem = WishlistItem.of(1L, 100L);
        when(wishlistItemRepository.findAllByMemberId(1L, null, 11)).thenReturn(
            List.of(wishlistItem));

        final ProductSnapshotResult snapshot = new ProductSnapshotResult(
            100L, "제목", "아티스트", "http://image", "장르", PressType.ORIGINAL, 2020, true, null);
        when(productService.getProductSnapshots(List.of(100L))).thenReturn(List.of(snapshot));

        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);
        final WishlistItemPageResult result = service.findPage(1L, null, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().productId()).isEqualTo(100L);
        assertThat(result.content().getFirst().title()).isEqualTo("제목");
        assertThat(result.content().getFirst().artistName()).isEqualTo("아티스트");
        assertThat(result.content().getFirst().coverImageUrl()).isEqualTo("http://image");
        assertThat(result.content().getFirst().releaseYear()).isEqualTo(2020);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void findAll은_참조하는_상품이_없으면_건너뛰고_에러를_발생시키지_않는다() {
        final WishlistItem wishlistItem = WishlistItem.of(1L, 999L);
        when(wishlistItemRepository.findAllByMemberId(1L, null, 11)).thenReturn(
            List.of(wishlistItem));
        when(productService.getProductSnapshots(List.of(999L))).thenReturn(List.of());

        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);
        final WishlistItemPageResult result = service.findPage(1L, null, 10);

        assertThat(result.content()).isEmpty();
    }

    @Test
    void findAll은_참조하는_상품이_비활성이면_건너뛰고_에러를_발생시키지_않는다() {
        final WishlistItem wishlistItem = WishlistItem.of(1L, 100L);
        when(wishlistItemRepository.findAllByMemberId(1L, null, 11)).thenReturn(
            List.of(wishlistItem));

        final ProductSnapshotResult snapshot = new ProductSnapshotResult(
            100L, "제목", "아티스트", "http://image", "장르", PressType.ORIGINAL, 2020, false, null);
        when(productService.getProductSnapshots(List.of(100L))).thenReturn(List.of(snapshot));

        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);
        final WishlistItemPageResult result = service.findPage(1L, null, 10);

        assertThat(result.content()).isEmpty();
    }

    @Test
    void findPage는_size보다_많이_조회되면_hasNext와_nextCursor를_설정한다() {
        final WishlistItem first = WishlistItem.of(1L, 100L);
        final WishlistItem second = WishlistItem.of(1L, 101L);
        final WishlistItem third = WishlistItem.of(1L, 102L);
        ReflectionTestUtils.setField(first, "id", 30L);
        ReflectionTestUtils.setField(second, "id", 20L);
        ReflectionTestUtils.setField(third, "id", 10L);
        when(wishlistItemRepository.findAllByMemberId(1L, null, 3))
            .thenReturn(List.of(first, second, third));

        final ProductSnapshotResult snapshotA = new ProductSnapshotResult(
            100L, "a", "아티스트", "http://image", "장르", PressType.ORIGINAL, 2020, true, null);
        final ProductSnapshotResult snapshotB = new ProductSnapshotResult(
            101L, "b", "아티스트", "http://image", "장르", PressType.ORIGINAL, 2020, true, null);
        when(productService.getProductSnapshots(List.of(100L, 101L)))
            .thenReturn(List.of(snapshotA, snapshotB));

        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);
        final WishlistItemPageResult result = service.findPage(1L, null, 2);

        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(20L);
    }

    @Test
    void findAll은_cursor를_그대로_리포지토리에_전달한다() {
        when(wishlistItemRepository.findAllByMemberId(1L, 50L, 11)).thenReturn(List.of());

        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);
        service.findPage(1L, 50L, 10);

        verify(wishlistItemRepository).findAllByMemberId(1L, 50L, 11);
    }

    @Test
    void add는_상품이_active일때_저장한다() {
        final WishlistItem saved = WishlistItem.of(1L, 100L);
        final ProductSnapshotResult snapshot = new ProductSnapshotResult(
            100L, "제목", "아티스트", "http://image", "장르", PressType.ORIGINAL, 2020, true, null);
        when(productService.getProductSnapshot(100L)).thenReturn(snapshot);
        when(wishlistItemRepository.save(any())).thenReturn(saved);

        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);
        final WishlistItem result = service.add(1L, 100L);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void add는_상품이_active가_아니면_예외를_던지고_저장하지_않는다() {
        final ProductSnapshotResult snapshot = new ProductSnapshotResult(
            100L, "제목", "아티스트", "http://image", "장르", PressType.ORIGINAL, 2020, false, null);
        when(productService.getProductSnapshot(100L)).thenReturn(snapshot);

        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);

        assertThatThrownBy(() -> service.add(1L, 100L)).isInstanceOf(
            ProductNotFoundException.class);
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void add는_상품이_존재하지_않으면_예외가_전파되고_저장하지_않는다() {
        when(productService.getProductSnapshot(100L)).thenThrow(new ProductNotFoundException());

        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);

        assertThatThrownBy(() -> service.add(1L, 100L)).isInstanceOf(
            ProductNotFoundException.class);
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void remove는_리포지토리에_삭제를_위임한다() {
        final WishlistService service = new WishlistService(wishlistItemRepository,
            productService);
        service.remove(1L, 100L);

        verify(wishlistItemRepository).deleteByMemberIdAndProductId(1L, 100L);
    }
}
