package site.coreservice.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import site.common.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 아티스트 (카탈로그 쓰기 모델). 이 컨텍스트가 원본을 소유한다.
 * PRODUCT가 artistId(Long)로 논리 참조한다 — 객체 참조 대신 ID 참조.
 */
@Entity
@Table(
        name = "artist",
        indexes = @Index(name = "idxArtistNormalizedName", columnList = "normalized_name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    /** 표기 변형 매칭용 별칭 (비틀즈 / The Beatles). MySQL json 컬럼. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aliases", columnDefinition = "json")
    private List<String> aliases = new ArrayList<>();

    private Artist(String name, String normalizedName, List<String> aliases) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.aliases = aliases != null ? new ArrayList<>(aliases) : new ArrayList<>();
    }

    public static Artist of(String name, String normalizedName, List<String> aliases) {
        return new Artist(name, normalizedName, aliases);
    }
}
