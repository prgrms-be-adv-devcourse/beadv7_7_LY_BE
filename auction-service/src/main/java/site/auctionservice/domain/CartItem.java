package site.auctionservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import site.common.entity.BaseEntity;

@Entity
@Table(name = "cart_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cart_items_auction_member",
            columnNames = {"auction_id", "member_id"})
    })
@Getter
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    protected CartItem() {
    }

    private CartItem(Long memberId, Long auctionId) {
        this.memberId = memberId;
        this.auctionId = auctionId;
    }

    public static CartItem of(Long memberId, Long auctionId) {
        return new CartItem(memberId, auctionId);
    }
}
