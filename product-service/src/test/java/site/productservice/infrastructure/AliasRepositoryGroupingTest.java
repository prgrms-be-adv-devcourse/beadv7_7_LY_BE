package site.productservice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.productservice.domain.ArtistAlias;
import site.productservice.domain.ProductAlias;

/**
 * 데이터베이스는 행 하나가 별칭 하나인 평평한 목록을 주고, 부르는 쪽은 소유자별로 묶인 모양이 필요하다.
 * 그 변환을 리포지토리가 맡는다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("별칭 묶음 조회")
class AliasRepositoryGroupingTest {

    @Mock
    private ProductAliasJpaRepository productAliasJpaRepository;

    @Mock
    private ArtistAliasJpaRepository artistAliasJpaRepository;

    @Test
    @DisplayName("한 아티스트에 별칭이 여럿이면 모두 담는다")
    void 아티스트_별칭_여럿() {
        // given
        given(artistAliasJpaRepository.findByArtistIdIn(List.of(1674L)))
                .willReturn(List.of(ArtistAlias.of(1674L, "비틀즈"), ArtistAlias.of(1674L, "Beatles")));
        ArtistAliasRepositoryImpl repository =
                new ArtistAliasRepositoryImpl(artistAliasJpaRepository);

        // when
        Map<Long, List<String>> names = repository.findNamesByArtistIds(List.of(1674L));

        // then
        assertThat(names.get(1674L)).containsExactlyInAnyOrder("비틀즈", "Beatles");
    }

    @Test
    @DisplayName("별칭이 없는 id는 키를 만들지 않는다")
    void 별칭_없으면_키_없음() {
        // given
        given(artistAliasJpaRepository.findByArtistIdIn(List.of(1674L, 9999L)))
                .willReturn(List.of(ArtistAlias.of(1674L, "비틀즈")));
        ArtistAliasRepositoryImpl repository =
                new ArtistAliasRepositoryImpl(artistAliasJpaRepository);

        // when
        Map<Long, List<String>> names = repository.findNamesByArtistIds(List.of(1674L, 9999L));

        // then
        assertThat(names).containsOnlyKeys(1674L);
    }

    @Test
    @DisplayName("빈 id 목록이면 조회하지 않는다")
    void 빈_목록은_조회_안_함() {
        // given
        ProductAliasRepositoryImpl repository =
                new ProductAliasRepositoryImpl(productAliasJpaRepository);

        // when
        Map<Long, List<String>> names = repository.findNamesByProductIds(List.of());

        // then
        // 찾을 것이 없는 질의를 굳이 보내지 않는다
        assertThat(names).isEmpty();
        then(productAliasJpaRepository).should(never()).findByProductIdIn(anyList());
    }

    @Test
    @DisplayName("상품 별칭은 상품 id로 묶는다")
    void 상품_별칭_묶기() {
        // given
        given(productAliasJpaRepository.findByProductIdIn(List.of(8130L, 73611L)))
                .willReturn(List.of(ProductAlias.of(8130L, "애비 로드"),
                        ProductAlias.of(73611L, "애비 로드")));
        ProductAliasRepositoryImpl repository =
                new ProductAliasRepositoryImpl(productAliasJpaRepository);

        // when
        Map<Long, List<String>> names = repository.findNamesByProductIds(List.of(8130L, 73611L));

        // then
        // 이름이 같아도 상품이 다르면 각각의 항목이다
        assertThat(names.get(8130L)).containsExactly("애비 로드");
        assertThat(names.get(73611L)).containsExactly("애비 로드");
    }
}
