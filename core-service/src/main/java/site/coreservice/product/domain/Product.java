package site.coreservice.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

/**
 * 상품 = Discogs 'Release' 수준의 특정 프레싱 (D-2). 레이블 + 카탈로그번호 + 발매국가 + 발매연도 + 프레스구분 + 포맷으로 1건.
 * <p>
 * 하드 식별 속성(카탈로그번호·발매국가·발매연도·프레스구분·포맷·artistId)은 생성 후 불변 — 고치면 '다른 상품을 가리키게 되는' 것이라 쌓인 시세 이력이 엉뚱한 릴리스에 붙는다.
 * 서술 속성(레이블·장르·커버·설명)만 {@link #updateDescriptive}로 수정한다.
 * <p>
 * 제목·정규화된제목은 표시/검색용이라 성격상 서술 속성이지만, 세미엔 상품 수정 API 자체가 없어 D1에선 생성자로만 설정한다.
 * 폴백 자연키(정규화된제목+아티스트+연도) 방어는 dedup·정규화를 구현하는 D3에서 정한다.
 * <p>
 * 아티스트는 artistId(Long)로 논리 참조한다 (객체 참조 금지). dedup 자연키: (normalized_catalog_number, format, release_country) —
 * MySQL은 유니크 인덱스에서 NULL을 서로 다른 값으로 취급하므로, 카탈로그번호가 없는 행은 자동으로 충돌하지 않는다(= 부분 유니크).
 */
@Entity
@Table(
        name = "product",
        uniqueConstraints = @UniqueConstraint(
                name = "ukProductNaturalKey",
                columnNames = {"normalized_catalog_number", "format", "release_country"}
        ),
        indexes = @Index(name = "idxProductNormalizedTitle", columnList = "normalized_title")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // === 식별 속성 (생성 후 불변) ===

    /** 원본 카탈로그 넘버 (nullable). 예: CL 1355 */
    @Column(name = "catalog_number")
    private String catalogNumber;

    /** 정규화된 카탈로그 넘버 — dedup 자연키 구성. 정규화는 쓰기 시점(세미는 시드가 직접 주입). */
    @Column(name = "normalized_catalog_number")
    private String normalizedCatalogNumber;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "release_country", nullable = false)
    private String releaseCountry;

    @Column(name = "release_year", nullable = false)
    private int releaseYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "press_type", nullable = false)
    private PressType pressType;

    /** LP / 2LP / 180g 등. */
    @Column(name = "format", nullable = false)
    private String format;

    // === 표시·검색 속성 (성격상 서술 · 세미엔 상품 수정 API 없어 D1은 생성자 전용 · 폴백키 방어는 D3) ===

    /** 표시용 원본 제목. */
    @Column(name = "title", nullable = false)
    private String title;

    /** 정규화된 제목 — 검색 인덱스용. 폴백 자연키(정규화된제목+아티스트+연도) 재료는 D3에서 다룬다. */
    @Column(name = "normalized_title", nullable = false)
    private String normalizedTitle;

    // === 서술 속성 (수정 가능) ===

    @Column(name = "label")
    private String label;

    @Column(name = "genre")
    private String genre;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** soft delete — 삭제해도 시세 이력을 보존하기 위해 비활성 플래그로만 처리. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    private Product(String catalogNumber, String normalizedCatalogNumber, Long artistId, String title,
            String normalizedTitle, String releaseCountry, int releaseYear, PressType pressType, String format,
            String label, String genre, String coverImage, String description) {
        this.catalogNumber = catalogNumber;
        this.normalizedCatalogNumber = normalizedCatalogNumber;
        this.artistId = artistId;
        this.title = title;
        this.normalizedTitle = normalizedTitle;
        this.releaseCountry = releaseCountry;
        this.releaseYear = releaseYear;
        this.pressType = pressType;
        this.format = format;
        this.label = label;
        this.genre = genre;
        this.coverImage = coverImage;
        this.description = description;
    }

    public static Product of(String catalogNumber, String normalizedCatalogNumber, Long artistId, String title,
            String normalizedTitle, String releaseCountry, int releaseYear, PressType pressType, String format,
            String label, String genre, String coverImage, String description) {
        return new Product(catalogNumber, normalizedCatalogNumber, artistId, title, normalizedTitle, releaseCountry,
                releaseYear, pressType, format, label, genre, coverImage, description);
    }

    /** 서술 속성만 수정한다. 식별 속성은 인자에 없다 — 바뀌면 다른 릴리스가 되기 때문. */
    public void updateDescriptive(String label, String genre, String coverImage, String description) {
        this.label = label;
        this.genre = genre;
        this.coverImage = coverImage;
        this.description = description;
    }

    public void deactivate() {
        this.active = false;
    }
}
