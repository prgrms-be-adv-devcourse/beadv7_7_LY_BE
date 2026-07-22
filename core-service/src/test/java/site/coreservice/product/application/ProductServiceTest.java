package site.coreservice.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.product.application.dto.ProductDetailResult;
import site.coreservice.product.application.dto.ProductSnapshotResult;
import site.coreservice.product.domain.Artist;
import site.coreservice.product.domain.ArtistRepository;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;
import site.coreservice.product.exception.ProductNotFoundException;
import site.coreservice.product.domain.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Artist artist;

    @BeforeEach
    void setUp() {
        artist = Artist.of("The Beatles", "thebeatles", List.of("비틀즈", "Beatles"));
        ReflectionTestUtils.setField(artist, "id", 3L);
        product = Product.of("PCS 7088", "pcs7088", 3L, "Abbey Road", "abbeyroad", "UK", 1969,
                PressType.ORIGINAL, "LP", "Apple Records", "Rock", null, "1969년 영국 오리지널 프레싱");
        ReflectionTestUtils.setField(product, "id", 55L);
    }

    @Test
    @DisplayName("상세 조회는 상품과 아티스트를 합성해 반환한다")
    void getProductDetail_상품과_아티스트를_합성해_반환() {
        // given
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));

        // when
        ProductDetailResult result = productService.getProductDetail(55L);

        // then
        assertThat(result.productId()).isEqualTo(55L);
        assertThat(result.catalogNumber()).isEqualTo("PCS 7088");
        assertThat(result.title()).isEqualTo("Abbey Road");
        assertThat(result.artist().artistId()).isEqualTo(3L);
        assertThat(result.artist().name()).isEqualTo("The Beatles");
        assertThat(result.artist().aliases()).containsExactly("비틀즈", "Beatles");
        assertThat(result.label()).isEqualTo("Apple Records");
        assertThat(result.country()).isEqualTo("UK");
        assertThat(result.releaseYear()).isEqualTo(1969);
        assertThat(result.pressType()).isEqualTo(PressType.ORIGINAL);
        assertThat(result.format()).isEqualTo("LP");
        assertThat(result.genre()).isEqualTo("Rock");
        assertThat(result.coverImageUrl()).isNull();
        assertThat(result.description()).isEqualTo("1969년 영국 오리지널 프레싱");
    }

    @Test
    @DisplayName("상세 조회는 없는 상품이면 상품없음 예외를 던진다")
    void getProductDetail_없는_상품이면_예외() {
        // given
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("상세 조회는 비활성 상품이면 상품없음 예외를 던진다 (사용자에겐 없는 상품)")
    void getProductDetail_비활성_상품이면_예외() {
        // given
        product.deactivate();
        given(productRepository.findById(55L)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(55L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("내부 조회는 비활성 상품도 active 플래그와 함께 반환한다 (검증 소스)")
    void getProduct_비활성_상품도_반환() {
        // given
        product.deactivate();
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));

        // when
        ProductSnapshotResult result = productService.getProduct(55L);

        // then
        assertThat(result.productId()).isEqualTo(55L);
        assertThat(result.active()).isFalse();
        assertThat(result.artistName()).isEqualTo("The Beatles");
        assertThat(result.mergedIntoId()).isNull();
    }

    @Test
    @DisplayName("내부 조회는 없는 상품이면 상품없음 예외를 던진다")
    void getProduct_없는_상품이면_예외() {
        // given
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("상품이 참조하는 아티스트가 없으면 정합성 예외를 던진다")
    void getProductDetail_아티스트_부재면_정합성_예외() {
        // given
        given(productRepository.findById(55L)).willReturn(Optional.of(product));
        given(artistRepository.findById(3L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(55L))
                .isInstanceOf(IllegalStateException.class);
    }
}
