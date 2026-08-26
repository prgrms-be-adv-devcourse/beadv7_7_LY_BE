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
 * 상품 제목의 다른 표기 (애비 로드 / Abbey Road). 어느 표기로 검색해도 같은 상품이 나오게 한다.
 * 사용자에게 보여주는 값이 아니라 검색이 쓰는 값이다. 검색 색인이 가져가는 내부 목록 조회에만 실리고,
 * 공개 API 응답에는 나가지 않는다.
 * <p>
 * 상품은 productId 숫자로만 참조한다 (연관관계 없음). 정규화 값은 생성 시 내부에서 계산하므로
 * "원본과 정규화 값이 어긋난 별칭"은 만들 수 없다. 같은 상품에 같은 정규화 별칭이 두 번 저장되는
 * 것은 유니크 제약이 막는다.
 */
@Entity
@Table(
        name = "product_alias",
        uniqueConstraints = @UniqueConstraint(
                name = "ukProductAliasOwnerNormalized",
                columnNames = {"product_id", "normalized_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductAlias extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 원본 표기 (예: 애비 로드). */
    @Column(name = "name", nullable = false)
    private String name;

    /** 검색용으로 표기를 통일한 값. */
    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    private ProductAlias(Long productId, String name, String normalizedName) {
        this.productId = productId;
        this.name = name;
        this.normalizedName = normalizedName;
    }

    public static ProductAlias of(Long productId, String name) {
        String normalizedName = TextNormalizer.normalize(name);
        if (normalizedName == null) {
            throw new IllegalArgumentException("정규화하면 아무 문자도 남지 않는 별칭입니다: " + name);
        }
        return new ProductAlias(productId, name, normalizedName);
    }
}
