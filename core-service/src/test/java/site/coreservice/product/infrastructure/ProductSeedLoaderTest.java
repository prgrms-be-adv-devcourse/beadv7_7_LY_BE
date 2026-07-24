package site.coreservice.product.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import site.coreservice.product.domain.Artist;
import site.coreservice.product.domain.ArtistAliasRepository;
import site.coreservice.product.domain.ArtistRepository;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.ProductAliasRepository;
import site.coreservice.product.domain.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductSeedLoaderTest {

    @Mock private ArtistRepository artistRepository;
    @Mock private ArtistAliasRepository artistAliasRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductAliasRepository productAliasRepository;
    @InjectMocks private ProductSeedLoader seedLoader;

    private Artist artistWithId(String name, long id) {
        Artist artist = Artist.of(name);
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
    @DisplayName("빈 DB에서는 아티스트·상품·별칭이 모두 저장된다")
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

        // then — 아티스트 3, 아티스트 별칭 4, 상품 6, 제목 별칭 6
        then(artistRepository).should(times(3)).save(any());
        then(artistAliasRepository).should(times(4)).save(any());
        then(productRepository).should(times(6)).save(any());
        then(productAliasRepository).should(times(6)).save(any());
    }

    @Test
    @DisplayName("전부 이미 있으면 아무것도 저장하지 않는다 (여러 번 실행해도 안전)")
    void run_전부_있으면_저장_없음() {
        // given
        given(artistRepository.findByNormalizedName("thebeatles"))
                .willReturn(Optional.of(artistWithId("The Beatles", 1L)));
        given(artistRepository.findByNormalizedName("pinkfloyd"))
                .willReturn(Optional.of(artistWithId("Pink Floyd", 2L)));
        given(artistRepository.findByNormalizedName("milesdavis"))
                .willReturn(Optional.of(artistWithId("Miles Davis", 3L)));
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
        given(artistRepository.findByNormalizedName("thebeatles"))
                .willReturn(Optional.of(artistWithId("The Beatles", 1L)));
        given(artistRepository.findByNormalizedName("pinkfloyd"))
                .willReturn(Optional.of(artistWithId("Pink Floyd", 2L)));
        given(artistRepository.findByNormalizedName("milesdavis"))
                .willReturn(Optional.of(artistWithId("Miles Davis", 3L)));
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
        then(artistAliasRepository).should(times(4)).save(any());
    }

    @Test
    @DisplayName("카탈로그번호 없는 상품은 예비 기준(폴백 키)으로 존재를 확인한다")
    void run_번호_없는_상품은_폴백_키_사용() {
        // given
        given(artistRepository.findByNormalizedName("thebeatles"))
                .willReturn(Optional.of(artistWithId("The Beatles", 1L)));
        given(artistRepository.findByNormalizedName("pinkfloyd"))
                .willReturn(Optional.of(artistWithId("Pink Floyd", 2L)));
        given(artistRepository.findByNormalizedName("milesdavis"))
                .willReturn(Optional.of(artistWithId("Miles Davis", 3L)));
        given(artistAliasRepository.hasAlias(any(), any())).willReturn(true);
        given(productRepository.findByNaturalKey(any(), any(), any()))
                .willReturn(Optional.of(productWithId(10L)));
        given(productRepository.findByFallbackNaturalKey(any(), any(), anyInt(), any(), any(), any()))
                .willReturn(Optional.of(productWithId(11L)));
        given(productAliasRepository.hasAlias(any(), any())).willReturn(true);

        // when
        seedLoader.run();

        // then — 부틀렉 1건만 폴백 키 경로를 탄다
        then(productRepository).should(times(1))
                .findByFallbackNaturalKey(eq("kumback"), eq(1L), eq(1969), eq("US"), eq("LP"),
                        eq(PressType.ORIGINAL));
    }
}
