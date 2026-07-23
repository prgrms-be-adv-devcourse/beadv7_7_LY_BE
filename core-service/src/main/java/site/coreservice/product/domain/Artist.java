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
 * 아티스트. 상품(Product)이 이 테이블을 artistId 숫자로 참조한다.
 * 아티스트 정보의 원본은 상품 도메인이 만들고 관리한다 (다른 도메인은 조회만).
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

    /** 같은 아티스트의 다른 표기들 (비틀즈 / The Beatles) — 어느 표기로 검색해도 찾히게 하기 위함. MySQL json 컬럼. */
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

    /**
     * 별칭 목록을 복사본으로 반환한다. DB에 별칭이 NULL로 저장된 행이 있으면 JPA가 DB 값을 읽어오면서
     * 필드 초기값(빈 리스트)을 null로 덮어써 버리므로, 호출한 쪽에 null이 새어나가지 않게 여기서 빈 목록으로 바꾼다.
     */
    public List<String> getAliases() {
        return aliases == null ? List.of() : List.copyOf(aliases);
    }
}
