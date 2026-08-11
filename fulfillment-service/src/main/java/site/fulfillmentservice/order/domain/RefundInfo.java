package site.fulfillmentservice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundInfo {

    private static final int MAX_IMAGE_COUNT = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_reason", length = 30)
    private RefundReason reason;

    @Column(name = "refund_description", length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "refund_images", columnDefinition = "json")
    private List<String> imageUrls;

    @Column(name = "refund_requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    private RefundInfo(RefundReason reason, String description, List<String> imageUrls, LocalDateTime requestedAt,
            LocalDateTime refundedAt) {
        this.reason = reason;
        this.description = description;
        this.imageUrls = imageUrls;
        this.requestedAt = requestedAt;
        this.refundedAt = refundedAt;
    }

    public static RefundInfo request(RefundReason reason, String description, List<String> imageUrls,
            LocalDateTime now) {
        validate(reason, imageUrls);
        List<String> images = (imageUrls == null) ? null : List.copyOf(imageUrls);
        return new RefundInfo(reason, description, images, now, null);
    }

    private static void validate(RefundReason reason, List<String> imageUrls) {
        Objects.requireNonNull(reason, "환불 사유는 필수입니다.");
        if (imageUrls != null && imageUrls.size() > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("환불 이미지는 최대 %d장까지 등록할 수 있습니다.".formatted(MAX_IMAGE_COUNT));
        }
    }

    public RefundInfo refund(LocalDateTime now) {
        return new RefundInfo(this.reason, this.description, this.imageUrls, this.requestedAt, now);
    }

}
