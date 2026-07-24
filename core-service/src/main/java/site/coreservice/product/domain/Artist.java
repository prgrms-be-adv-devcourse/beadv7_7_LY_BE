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
import site.common.entity.BaseEntity;

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

    private Artist(String name, String normalizedName) {
        this.name = name;
        this.normalizedName = normalizedName;
    }

    public static Artist of(String name) {
        String normalizedName = TextNormalizer.normalize(name);
        if (normalizedName == null) {
            throw new IllegalArgumentException("정규화하면 아무 문자도 남지 않는 아티스트 이름입니다: " + name);
        }
        return new Artist(name, normalizedName);
    }
}
