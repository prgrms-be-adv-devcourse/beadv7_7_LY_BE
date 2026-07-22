package site.coreservice.auction.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import site.common.entity.BaseEntity;

@Entity
@Table(name = "cart_items")
@Getter
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long productId;

    protected CartItem() {
    }

    public CartItem(Long memberId, Long productId) {
        this.memberId = memberId;
        this.productId = productId;
    }

}
