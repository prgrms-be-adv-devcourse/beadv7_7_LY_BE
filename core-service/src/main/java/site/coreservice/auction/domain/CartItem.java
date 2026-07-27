package site.coreservice.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "auction_id")
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
