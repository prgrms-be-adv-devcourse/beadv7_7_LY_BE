package site.productservice.infrastructure.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.productservice.domain.Artist;
import site.productservice.domain.ArtistAliasRepository;
import site.productservice.domain.ArtistRepository;
import site.productservice.domain.PressType;
import site.productservice.domain.Product;
import site.productservice.domain.ProductAliasRepository;
import site.productservice.domain.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductSeedLoaderTest {

    // 기대 수치는 원장(ProductSeedData)에서 계산한다 — 데이터를 늘려도 테스트를 고칠 필요가 없도록
    private static final int ARTIST_COUNT = ProductSeedData.ARTISTS.size();
    private static final int ARTIST_ALIAS_COUNT = ProductSeedData.ARTISTS.stream()
            .mapToInt(artist -> artist.aliases().size())
            .sum();
    private static final int PRODUCT_COUNT = ProductSeedData.PRODUCTS.size();
    private static final int PRODUCT_ALIAS_COUNT = ProductSeedData.PRODUCTS.stream()
            .mapToInt(product -> product.titleAliases().size())
            .sum();
    private static final int FALLBACK_KEY_PRODUCT_COUNT = (int) ProductSeedData.PRODUCTS.stream()
            .filter(product -> product.catalogNumber() == null)
            .count();

    @Mock private ArtistRepository artistRepository;
    @Mock private ArtistAliasRepository artistAliasRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductAliasRepository productAliasRepository;
    @InjectMocks private ProductSeedLoader seedLoader;

    private Artist artistWithId(long id) {
        Artist artist = Artist.of("존재하는 아티스트");
        ReflectionTestUtils.setField(artist, "id", id);
        return artist;
    }

    private Product productWithId(long id) {
        Product product = Product.of("PCS 7088", 1L, "Abbey Road", "UK", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Test
    @DisplayName("빈 DB에서는 아티스트·상품·별칭이 원장 수만큼 모두 저장된다")
    void run_빈_DB면_전부_저장() {
        // given — 모든 조회가 "없음"
        given(artistRepository.findByNormalizedName(any())).willReturn(Optional.empty());
        given(artistRepository.save(any())).willAnswer(inv -> {
            Artist saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        given(artistAliasRepository.hasAlias(any(), any())).willReturn(false);
        given(productRepository.findByNaturalKey(any(), any(), any())).willReturn(Optional.empty());
        given(productRepository.findByFallbackNaturalKey(any(), any(), anyInt(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(productRepository.save(any())).willAnswer(inv -> {
            Product saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
        given(productAliasRepository.hasAlias(any(), any())).willReturn(false);

        // when
        seedLoader.run();

        // then
        then(artistRepository).should(times(ARTIST_COUNT)).save(any());
        then(artistAliasRepository).should(times(ARTIST_ALIAS_COUNT)).save(any());
        then(productRepository).should(times(PRODUCT_COUNT)).save(any());
        then(productAliasRepository).should(times(PRODUCT_ALIAS_COUNT)).save(any());
    }

    @Test
    @DisplayName("전부 이미 있으면 아무것도 저장하지 않는다 (여러 번 실행해도 안전)")
    void run_전부_있으면_저장_없음() {
        // given
        given(artistRepository.findByNormalizedName(any())).willReturn(Optional.of(artistWithId(1L)));
        given(artistAliasRepository.hasAlias(any(), any())).willReturn(true);
        given(productRepository.findByNaturalKey(any(), any(), any()))
                .willReturn(Optional.of(productWithId(10L)));
        given(productRepository.findByFallbackNaturalKey(any(), any(), anyInt(), any(), any(), any()))
                .willReturn(Optional.of(productWithId(11L)));
        given(productAliasRepository.hasAlias(any(), any())).willReturn(true);

        // when
        seedLoader.run();

        // then
        then(artistRepository).should(never()).save(any());
        then(artistAliasRepository).should(never()).save(any());
        then(productRepository).should(never()).save(any());
        then(productAliasRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("아티스트가 이미 있어도 없는 별칭은 채워진다 (확인 단위 = 자원 각각)")
    void run_기존_아티스트여도_별칭은_채움() {
        // given — 아티스트는 전부 존재, 별칭은 전부 없음
        given(artistRepository.findByNormalizedName(any())).willReturn(Optional.of(artistWithId(1L)));
        given(artistAliasRepository.hasAlias(any(), any())).willReturn(false);
        given(productRepository.findByNaturalKey(any(), any(), any()))
                .willReturn(Optional.of(productWithId(10L)));
        given(productRepository.findByFallbackNaturalKey(any(), any(), anyInt(), any(), any(), any()))
                .willReturn(Optional.of(productWithId(11L)));
        given(productAliasRepository.hasAlias(any(), any())).willReturn(true);

        // when
        seedLoader.run();

        // then
        then(artistRepository).should(never()).save(any());
        then(artistAliasRepository).should(times(ARTIST_ALIAS_COUNT)).save(any());
    }

    @Test
    @DisplayName("카탈로그번호 없는 상품만 예비 기준(폴백 키)으로, 나머지는 번호로 존재를 확인한다")
    void run_번호_유무에_따라_확인_경로_분기() {
        // given
        given(artistRepository.findByNormalizedName(any())).willReturn(Optional.of(artistWithId(1L)));
        given(artistAliasRepository.hasAlias(any(), any())).willReturn(true);
        given(productRepository.findByNaturalKey(any(), any(), any()))
                .willReturn(Optional.of(productWithId(10L)));
        given(productRepository.findByFallbackNaturalKey(any(), any(), anyInt(), any(), any(), any()))
                .willReturn(Optional.of(productWithId(11L)));
        given(productAliasRepository.hasAlias(any(), any())).willReturn(true);

        // when
        seedLoader.run();

        // then
        then(productRepository).should(times(FALLBACK_KEY_PRODUCT_COUNT))
                .findByFallbackNaturalKey(any(), any(), anyInt(), any(), any(), any());
        then(productRepository).should(times(PRODUCT_COUNT - FALLBACK_KEY_PRODUCT_COUNT))
                .findByNaturalKey(any(), any(), any());
    }
}
