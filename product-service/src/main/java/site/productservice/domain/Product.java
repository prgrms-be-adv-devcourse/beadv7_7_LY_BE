package site.productservice.domain;

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
import site.common.text.TextNormalizer;

/**
 * 상품 1건 = 특정 시기·국가에서 찍어낸 LP 한 종류 (예: Abbey Road 1969년 영국 초판).
 * 같은 앨범이라도 카탈로그번호·발매국가·발매연도·프레스구분·포맷이 다르면 서로 다른 상품이다.
 * <p>
 * 위의 "어떤 음반인지 가리키는" 속성들은 한번 저장하면 고칠 수 없다. 고치는 순간 다른 음반을 가리키게 되어,
 * 그동안 쌓인 거래 시세가 엉뚱한 음반에 붙기 때문이다. 레이블·장르·커버·설명 같은 부가 정보만
 * {@link #updateDescriptive}로 수정한다.
 * <p>
 * 같은 상품이 두 번 등록되는 것은 (정규화된 카탈로그번호, 포맷, 발매국가) 유니크 제약이 막는다.
 * 카탈로그번호가 없는 음반(부틀렉·자체 제작반 등)은 이 제약을 그냥 지나칠 수 있는데 — MySQL이 null끼리는
 * 유니크 검사를 하지 않기 때문 — 그런 음반끼리의 중복은 저장 전에 별도 기준
 * (정규화 제목 + 아티스트 + 발매연도 + 발매국가 + 포맷 + 프레스구분)으로 조회해서 막는다.
 * <p>
 * 아티스트는 객체가 아니라 artistId 숫자로만 참조한다 (다른 영역의 객체를 직접 들고 다니지 않는 팀 규칙).
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

    // === 어떤 음반인지 가리키는 속성 — 한번 저장하면 수정 불가 ===

    /** 음반사가 부여한 카탈로그번호 원문 (예: CL 1355). 번호가 없거나 알 수 없는 음반은 null. */
    @Column(name = "catalog_number")
    private String catalogNumber;

    /** 중복 확인·검색용으로 표기를 통일(소문자화·기호 제거)한 카탈로그번호. 원문이 없으면 null. */
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

    // === 제목 — 성격은 부가 정보에 가깝지만, 상품 수정 API가 없어 생성 시에만 설정 ===

    /** 화면 표시용 원본 제목. */
    @Column(name = "title", nullable = false)
    private String title;

    /** 검색·중복 확인용으로 표기를 통일한 제목. */
    @Column(name = "normalized_title", nullable = false)
    private String normalizedTitle;

    // === 부가 정보 (수정 가능) ===

    @Column(name = "label")
    private String label;

    @Column(name = "genre")
    private String genre;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 삭제 대신 쓰는 비활성 표시. 행을 실제로 지우면 이 상품에 쌓인 시세 이력까지 잃기 때문. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    // === 외부 카탈로그(Discogs) 연동 값 — 상품 카탈로그를 Discogs 데이터로 채우면서 추가 ===
    // 셋 다 우리가 만드는 값이 아니라 원본 식별자를 그대로 보관하는 자리라 수정 대상이 아니다.
    // 손으로 등록한 상품에는 값이 없을 수 있어 전부 nullable.

    /** Discogs 릴리스 식별자. 카탈로그를 다시 적재할 때 이 값으로 같은 행을 찾는다 —
     *  없으면 적재할 때마다 id가 새로 매겨져 이 상품을 가리키던 경매·시세·찜이 전부 어긋난다. */
    @Column(name = "discogs_release_id", unique = true)
    private Long discogsReleaseId;

    /** 같은 앨범의 여러 판(초판·재발매·각국 프레싱)을 묶는 식별자.
     *  Discogs에는 초판/재발매 구분 값이 따로 없어서, 이 묶음 안에서 발매연도가 가장 이른 것을
     *  초판으로 본다 — {@link #pressType}을 정하는 유일한 근거다. */
    @Column(name = "discogs_master_id")
    private Long discogsMasterId;

    /** Discogs가 적어 둔 판 정보 원문 (예: "LP, Album, Reissue, Remastered, 180g, Green colored vinyl").
     *  색깔·중량·한정 여부는 실제 시세에 영향을 주므로 버리지 않고 통째로 보관한다.
     *  다만 같은 뜻을 여러 표기로 적어(180g / 180 gr / 180 gram) 그대로는 조건으로 쓸 수 없다 —
     *  필요해지면 이 값을 원본 삼아 따로 정리한 컬럼을 만든다. 검색·필터에 직접 쓰지 않는다. */
    @Column(name = "discogs_descriptors", length = 500)
    private String discogsDescriptors;

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

    public static Product of(String catalogNumber, Long artistId, String title, String releaseCountry,
            int releaseYear, PressType pressType, String format, String label, String genre, String coverImage,
            String description) {
        // 정규화해서 아무 문자도 남지 않는 카탈로그번호("---", 공백 등)는 "없음"으로 취급 — 원본도 null로 통일
        String normalizedCatalogNumber = TextNormalizer.normalize(catalogNumber);
        String rawCatalogNumber = (normalizedCatalogNumber == null) ? null : catalogNumber;
        String normalizedTitle = TextNormalizer.normalize(title);
        if (normalizedTitle == null) {
            throw new IllegalArgumentException("정규화하면 아무 문자도 남지 않는 제목입니다: " + title);
        }
        return new Product(rawCatalogNumber, normalizedCatalogNumber, artistId, title,
                normalizedTitle, releaseCountry, releaseYear, pressType, format, label, genre, coverImage,
                description);
    }

    /** 부가 정보만 수정한다. 음반을 가리키는 속성은 인자에 없다 — 그게 바뀌면 다른 상품이 되기 때문. */
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
