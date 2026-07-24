package site.coreservice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemSnapshot {

    @Column(name = "product_album_title", nullable = false)
    private String albumTitle;

    @Column(name = "product_artist_name", nullable = false)
    private String artistName;

    @Column(name = "product_release_year")
    private Integer releaseYear;

    @Column(name = "product_press_type")
    private String pressType;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_grade", nullable = false, length = 20)
    private ConditionGrade conditionGrade;

    @Column(name = "representative_image_url", nullable = false, length = 500)
    private String representativeImageUrl;

    private OrderItemSnapshot(String albumTitle, String artistName, Integer releaseYear, String pressType,
            ConditionGrade conditionGrade, String representativeImageUrl) {
        validate(albumTitle, artistName, conditionGrade, representativeImageUrl);
        this.albumTitle = albumTitle;
        this.artistName = artistName;
        this.releaseYear = releaseYear;
        this.pressType = pressType;
        this.conditionGrade = conditionGrade;
        this.representativeImageUrl = representativeImageUrl;
    }

    public static OrderItemSnapshot of(String albumTitle, String artistName, Integer releaseYear,
            String pressType, ConditionGrade conditionGrade, String representativeImageUrl) {
        return new OrderItemSnapshot(albumTitle, artistName, releaseYear, pressType, conditionGrade,
                representativeImageUrl);
    }

    private static void validate(String albumTitle, String artistName, ConditionGrade conditionGrade,
            String representativeImageUrl) {
        Objects.requireNonNull(albumTitle, "albumTitle은 null일 수 없습니다.");
        Objects.requireNonNull(artistName, "artistName은 null일 수 없습니다.");
        Objects.requireNonNull(conditionGrade, "conditionGrade는 null일 수 없습니다.");
        Objects.requireNonNull(representativeImageUrl, "representativeImageUrl은 null일 수 없습니다.");
    }
}
