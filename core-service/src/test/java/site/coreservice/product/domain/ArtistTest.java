package site.coreservice.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArtistTest {

    @Test
    @DisplayName("of는 aliases가 null이면 빈 리스트로 만든다 (NPE 방지)")
    void of_aliases가_null이면_빈리스트() {
        // given & when
        Artist artist = Artist.of("The Beatles", "thebeatles", null);

        // then
        assertThat(artist.getAliases()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("of는 aliases를 방어적 복사한다 — 원본을 나중에 바꿔도 엔티티는 영향 없음")
    void of_aliases를_방어적_복사한다() {
        // given
        List<String> source = new ArrayList<>(List.of("비틀즈", "The Beatles"));

        // when
        Artist artist = Artist.of("The Beatles", "thebeatles", source);
        source.add("오염된_별칭");

        // then
        assertThat(artist.getAliases()).containsExactly("비틀즈", "The Beatles");
    }

    @Test
    @DisplayName("getAliases는 하이드레이션으로 필드가 null이 돼도 빈 리스트를 반환한다 (DB aliases NULL 행 방어)")
    void getAliases_필드가_null이어도_빈리스트() {
        // given: nullable JSON 컬럼은 Hibernate가 필드 초기화를 DB의 NULL로 덮어쓴다 — 그 상태를 재현
        Artist artist = Artist.of("The Beatles", "thebeatles", List.of("비틀즈"));
        ReflectionTestUtils.setField(artist, "aliases", null);

        // when & then
        assertThat(artist.getAliases()).isNotNull().isEmpty();
    }
}
