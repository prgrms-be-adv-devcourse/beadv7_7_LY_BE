package site.auctionservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.util.List;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemInfo {

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 20)
    private ItemCondition condition;

    @Column(name = "item_description", length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_images", columnDefinition = "json")
    private List<String> imageUrls;

    private ItemInfo(ItemCondition condition, String description, List<String> imageUrls) {
        this.condition = condition;
        this.description = description;
        this.imageUrls = imageUrls;
    }

    public static ItemInfo of(ItemCondition condition, String description, List<String> imageUrls) {
        Objects.requireNonNull(condition, "경매 상품 상태는 null일 수 없습니다.");
        if (description != null && (description.length() < AuctionPolicy.MIN_DESC_LENGTH
            || description.length() > AuctionPolicy.MAX_DESC_LENGTH)) {
            throw new AuctionException(AuctionErrorCode.ITEM_DESCRIPTION_LENGTH_INVALID);
        }
        List<String> images = (imageUrls == null) ? null : List.copyOf(imageUrls);
        if (images != null && images.size() > AuctionPolicy.MAX_IMAGE_COUNT) {
            throw new AuctionException(AuctionErrorCode.ITEM_IMAGE_COUNT_EXCEEDED);
        }
        return new ItemInfo(condition, description, images);
    }

    public ItemInfo withDescription(String newDescription) {
        return new ItemInfo(this.condition, newDescription, this.imageUrls);
    }

    public ItemInfo withImages(List<String> newImages) {
        return new ItemInfo(this.condition, this.description, newImages);
    }

    public ItemInfo withCondition(ItemCondition newCondition) {
        return new ItemInfo(newCondition, this.description, this.imageUrls);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemInfo i)) {
            return false;
        }
        return condition == i.condition
            && Objects.equals(description, i.description)
            && Objects.equals(imageUrls, i.imageUrls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition, description, imageUrls);
    }
}
