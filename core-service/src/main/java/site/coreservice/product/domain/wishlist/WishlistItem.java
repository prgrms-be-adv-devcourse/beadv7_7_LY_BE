package site.coreservice.product.domain.wishlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

@Entity
@Table(name = "wishlist_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_wishlist_item_product_member",
        columnNames = {"product_id", "member_id"}
    )
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WishlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private WishlistItem(Long memberId, Long productId) {
        this.memberId = memberId;
        this.productId = productId;
    }

    public static WishlistItem of(Long memberId, Long productId) {
        return new WishlistItem(memberId, productId);
    }

}
