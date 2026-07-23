package site.coreservice.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

@Entity
@Table(name = "wishlist_items")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WishlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long memberId;

    @Column
    private Long productId;

    public WishlistItem(Long memberId, Long productId) {
        this.memberId = memberId;
        this.productId = productId;
    }

}
