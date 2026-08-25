package site.productservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;
import site.common.text.TextNormalizer;

/**
 * 아티스트의 다른 표기 (비틀즈 / The Beatles). 어느 표기로 검색해도 같은 아티스트의 상품이
 * 나오게 한다.
 * 검색 전용 데이터라 API 응답엔 노출하지 않는다.
 * <p>
 * 아티스트는 artistId 숫자로만 참조한다 (연관관계 없음). 정규화 값은 생성 시 내부에서 계산하므로
 * "원본과 정규화 값이 어긋난 별칭"은 만들 수 없다. 같은 아티스트에 같은 정규화 별칭이 두 번
 * 저장되는 것은 유니크 제약이 막는다.
 */
@Entity
@Table(
        name = "artist_alias",
        uniqueConstraints = @UniqueConstraint(
                name = "ukArtistAliasOwnerNormalized",
                columnNames = {"artist_id", "normalized_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistAlias extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    /** 원본 표기 (예: 비틀즈). */
    @Column(name = "name", nullable = false)
    private String name;

    /** 검색용으로 표기를 통일한 값. */
    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    private ArtistAlias(Long artistId, String name, String normalizedName) {
        this.artistId = artistId;
        this.name = name;
        this.normalizedName = normalizedName;
    }

    public static ArtistAlias of(Long artistId, String name) {
        String normalizedName = TextNormalizer.normalize(name);
        if (normalizedName == null) {
            throw new IllegalArgumentException("정규화하면 아무 문자도 남지 않는 별칭입니다: " + name);
        }
        return new ArtistAlias(artistId, name, normalizedName);
    }
}
