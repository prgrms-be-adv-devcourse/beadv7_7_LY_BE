package site.productservice.application.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.productservice.application.ProductService;
import site.productservice.application.dto.ProductSnapshotResult;
import site.productservice.application.dto.wishlist.WishlistItemPage;
import site.productservice.application.dto.wishlist.WishlistItemPageResult;
import site.productservice.application.dto.wishlist.WishlistItemResult;
import site.productservice.application.dto.wishlist.WishlistProductResult;
import site.productservice.domain.PressType;
import site.productservice.domain.wishlist.WishlistItem;
import site.productservice.exception.ProductNotFoundException;

/**
 * 상품 애그리거트와 엮이는 부분(담을 수 있는 상품인지 확인, 조회 결과에 상품 정보 채우기, 비활성 상품 걸러내기)과 이벤트 발행 시점을 검증한다. 커서 페이징 계산은
 * WishlistService로 옮겨가서 WishlistServiceTest에 있다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistServiceFacade")
class WishlistServiceFacadeTest {

    @Mock
    private WishlistService wishlistService;

    @Mock
    private ProductService productService;

    @Mock
    private WishlistEventPublisher wishlistEventPublisher;

    private WishlistServiceFacade facade() {
        return new WishlistServiceFacade(wishlistService, productService, wishlistEventPublisher);
    }

    @Test
    @DisplayName("각 항목에 해당 상품 정보를 채워 반환한다")
    void findPage은_상품_정보를_채워_반환한다() {
        final WishlistItem item = WishlistItem.of(1L, 100L);
        when(wishlistService.findPage(1L, null, 10))
            .thenReturn(new WishlistItemPage(List.of(item), null, false));
        when(productService.getProductSnapshots(List.of(100L))).thenReturn(List.of(snapshot(true)));

        final WishlistItemPageResult result = facade().findPage(1L, null, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().productId()).isEqualTo(100L);
        assertThat(result.content().getFirst().title()).isEqualTo("제목");
        assertThat(result.content().getFirst().artistName()).isEqualTo("아티스트");
        assertThat(result.content().getFirst().coverImageUrl()).isEqualTo("http://image");
        assertThat(result.content().getFirst().releaseYear()).isEqualTo(2020);
    }

    @Test
    @DisplayName("페이지 정보(nextCursor, hasNext)를 그대로 전달한다")
    void findPage은_페이지_정보를_그대로_전달한다() {
        when(wishlistService.findPage(1L, null, 10))
            .thenReturn(new WishlistItemPage(List.of(), 20L, true));

        final WishlistItemPageResult result = facade().findPage(1L, null, 10);

        assertThat(result.nextCursor()).isEqualTo(20L);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("참조하는 상품이 없으면 건너뛰고 에러를 발생시키지 않는다")
    void findPage은_상품이_없으면_건너뛴다() {
        final WishlistItem item = WishlistItem.of(1L, 999L);
        when(wishlistService.findPage(1L, null, 10))
            .thenReturn(new WishlistItemPage(List.of(item), null, false));
        when(productService.getProductSnapshots(List.of(999L))).thenReturn(List.of());

        final WishlistItemPageResult result = facade().findPage(1L, null, 10);

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("참조하는 상품이 비활성이면 건너뛰고 에러를 발생시키지 않는다")
    void findPage은_상품이_비활성이면_건너뛴다() {
        final WishlistItem item = WishlistItem.of(1L, 100L);
        when(wishlistService.findPage(1L, null, 10))
            .thenReturn(new WishlistItemPage(List.of(item), null, false));
        when(productService.getProductSnapshots(List.of(100L))).thenReturn(
            List.of(snapshot(false)));

        final WishlistItemPageResult result = facade().findPage(1L, null, 10);

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("최근 담은 상품을 커서 없이 limit만큼 조회한다")
    void findRecentProducts는_최근순으로_조회한다() {
        when(wishlistService.findPage(1L, null, 50))
            .thenReturn(new WishlistItemPage(List.of(WishlistItem.of(1L, 100L)), null, false));
        when(productService.getProductSnapshots(List.of(100L))).thenReturn(List.of(snapshot(true)));

        final List<WishlistProductResult> result = facade().findRecentProducts(1L, 50);

        assertThat(result).extracting(WishlistProductResult::productId).containsExactly(100L);
        // 임베딩 텍스트의 재료가 되는 필드들 — 화면용 응답에는 없는 것들이다.
        assertThat(result.getFirst().genre()).isEqualTo("장르");
        assertThat(result.getFirst().label()).isEqualTo("레이블");
        assertThat(result.getFirst().releaseCountry()).isEqualTo("한국");
        assertThat(result.getFirst().pressType()).isEqualTo("ORIGINAL");
    }

    // 벡터를 평균 내는 지금은 순서가 무의미하지만, 관심사를 묶을 때는 "최근에 담은 것"이 신호가 된다.
    // 상품 배치 조회는 위시리스트 순서를 지켜주지 않으므로 되맞추는 로직이 필요하고, 그게 이 테스트다.
    @Test
    @DisplayName("상품 조회 결과가 뒤섞여 와도 담은 순서를 유지한다")
    void findRecentProducts는_담은_순서를_유지한다() {
        when(wishlistService.findPage(1L, null, 50)).thenReturn(new WishlistItemPage(
            List.of(WishlistItem.of(1L, 300L), WishlistItem.of(1L, 100L),
                WishlistItem.of(1L, 200L)), null, false));
        when(productService.getProductSnapshots(List.of(300L, 100L, 200L))).thenReturn(
            List.of(snapshot(100L, true), snapshot(200L, true), snapshot(300L, true)));

        final List<WishlistProductResult> result = facade().findRecentProducts(1L, 50);

        assertThat(result).extracting(WishlistProductResult::productId)
            .containsExactly(300L, 100L, 200L);
    }

    @Test
    @DisplayName("비활성이거나 없는 상품은 빼고 돌려준다")
    void findRecentProducts는_비활성과_누락을_거른다() {
        when(wishlistService.findPage(1L, null, 50)).thenReturn(new WishlistItemPage(
            List.of(WishlistItem.of(1L, 100L), WishlistItem.of(1L, 200L),
                WishlistItem.of(1L, 999L)), null, false));
        when(productService.getProductSnapshots(List.of(100L, 200L, 999L))).thenReturn(
            List.of(snapshot(100L, true), snapshot(200L, false)));

        final List<WishlistProductResult> result = facade().findRecentProducts(1L, 50);

        // 비활성 상품은 추천 결과에도 안 나오므로 읽는 쪽이 받아도 할 일이 없다.
        assertThat(result).extracting(WishlistProductResult::productId).containsExactly(100L);
    }

    @Test
    @DisplayName("담은 상품이 없으면 빈 목록을 돌려준다")
    void findRecentProducts는_비어있으면_빈_목록이다() {
        when(wishlistService.findPage(1L, null, 50))
            .thenReturn(new WishlistItemPage(List.of(), null, false));
        when(productService.getProductSnapshots(List.of())).thenReturn(List.of());

        assertThat(facade().findRecentProducts(1L, 50)).isEmpty();
    }

    @Test
    @DisplayName("추가는 저장을 위임하고 변경 이벤트를 발행한다")
    void add는_저장을_위임하고_이벤트를_발행한다() {
        final WishlistItem saved = WishlistItem.of(1L, 100L);
        ReflectionTestUtils.setField(saved, "id", 7L);
        when(productService.getProductSnapshot(100L)).thenReturn(snapshot(true));
        when(wishlistService.add(1L, 100L)).thenReturn(saved);

        final WishlistItemResult result = facade().add(1L, 100L);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.productId()).isEqualTo(100L);
        verify(wishlistService).add(1L, 100L);
        verify(wishlistEventPublisher).publishAdded(1L);
    }

    @Test
    @DisplayName("추가 결과에도 상품 정보를 채워 반환한다 — 검증하며 조회한 스냅샷을 그대로 쓴다")
    void add는_상품_정보를_채워_반환한다() {
        final WishlistItem saved = WishlistItem.of(1L, 100L);
        when(productService.getProductSnapshot(100L)).thenReturn(snapshot(true));
        when(wishlistService.add(1L, 100L)).thenReturn(saved);

        final WishlistItemResult result = facade().add(1L, 100L);

        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.artistName()).isEqualTo("아티스트");
        assertThat(result.coverImageUrl()).isEqualTo("http://image");
        assertThat(result.releaseYear()).isEqualTo(2020);
        // 검증 때 조회한 스냅샷을 재사용하므로 상품 조회는 한 번뿐이다.
        verify(productService).getProductSnapshot(100L);
    }

    @Test
    @DisplayName("상품이 active가 아니면 예외를 던지고 저장하지 않는다")
    void add는_상품이_active가_아니면_예외를_던진다() {
        when(productService.getProductSnapshot(100L)).thenReturn(snapshot(false));

        assertThatThrownBy(() -> facade().add(1L, 100L))
            .isInstanceOf(ProductNotFoundException.class);
        verify(wishlistService, never()).add(anyLong(), anyLong());
        verify(wishlistEventPublisher, never()).publishAdded(anyLong());
    }

    @Test
    @DisplayName("상품이 존재하지 않으면 예외가 전파되고 저장하지 않는다")
    void add는_상품이_없으면_예외가_전파된다() {
        when(productService.getProductSnapshot(100L)).thenThrow(new ProductNotFoundException());

        assertThatThrownBy(() -> facade().add(1L, 100L))
            .isInstanceOf(ProductNotFoundException.class);
        verify(wishlistService, never()).add(anyLong(), anyLong());
        verify(wishlistEventPublisher, never()).publishAdded(anyLong());
    }

    @Test
    @DisplayName("삭제는 위임하고 변경 이벤트를 발행한다")
    void remove는_삭제를_위임하고_이벤트를_발행한다() {
        facade().remove(1L, 100L);

        verify(wishlistService).remove(1L, 100L);
        verify(wishlistEventPublisher).publishRemoved(1L);
    }

    // 저장이 실패하면(=트랜잭션이 롤백되면) 바뀐 게 없으므로 이벤트도 나가면 안 된다.
    // 발행 호출이 저장 뒤에 있다는 코드 배치로 보장되지만, 순서를 바꾸면 조용히 깨지는 부분이라 고정해 둔다.
    @Test
    @DisplayName("저장이 실패하면 이벤트를 발행하지 않는다")
    void 저장이_실패하면_이벤트를_발행하지_않는다() {
        when(productService.getProductSnapshot(100L)).thenReturn(snapshot(true));
        when(wishlistService.add(1L, 100L)).thenThrow(new IllegalStateException("저장 실패"));

        assertThatThrownBy(() -> facade().add(1L, 100L))
            .isInstanceOf(IllegalStateException.class);
        verify(wishlistEventPublisher, never()).publishAdded(anyLong());
    }

    private ProductSnapshotResult snapshot(final boolean active) {
        return snapshot(100L, active);
    }

    private ProductSnapshotResult snapshot(final Long productId, final boolean active) {
        return new ProductSnapshotResult(productId, "제목", "아티스트", "http://image", "장르",
            "레이블", PressType.ORIGINAL, 2020, "한국", active, null);
    }
}
