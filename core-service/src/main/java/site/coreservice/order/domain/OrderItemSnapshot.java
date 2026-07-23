package site.coreservice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemSnapshot {

    @Column(name = "product_album_title", nullable = false)
    private String albumTitle;

    @Column(name = "product_artist_name", nullable = false)
    private String artistName;

    @Column(name = "product_release_year")
    private Integer releaseYear;

    @Column(name = "product_release_country")
    private String releaseCountry;

    @Column(name = "product_press_type")
    private String pressType;

    @Column(name = "product_format")
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_grade", nullable = false, length = 20)
    private ConditionGrade conditionGrade;

    @Column(name = "representative_image_url", nullable = false, length = 500)
    private String representativeImageUrl;

    private OrderItemSnapshot(String albumTitle, String artistName, Integer releaseYear, String releaseCountry,
            String pressType, String format, ConditionGrade conditionGrade, String representativeImageUrl) {
        this.albumTitle = albumTitle;
        this.artistName = artistName;
        this.releaseYear = releaseYear;
        this.releaseCountry = releaseCountry;
        this.pressType = pressType;
        this.format = format;
        this.conditionGrade = conditionGrade;
        this.representativeImageUrl = representativeImageUrl;
    }

    public static OrderItemSnapshot of(String albumTitle, String artistName, Integer releaseYear,
            String releaseCountry, String pressType, String format, ConditionGrade conditionGrade,
            String representativeImageUrl) {
        return new OrderItemSnapshot(albumTitle, artistName, releaseYear, releaseCountry, pressType, format,
                conditionGrade, representativeImageUrl);
    }
}
