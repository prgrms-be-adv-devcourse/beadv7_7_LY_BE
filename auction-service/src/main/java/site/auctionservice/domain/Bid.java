package site.auctionservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "bid")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long bidderId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "bid_amount", nullable = false))
    private Money amount;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private LocalDateTime placedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BidOutcome outcome;

    private Bid(Long auctionId, Long bidderId, Money amount, LocalDateTime placedAt, BidOutcome outcome) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.placedAt = placedAt;
        this.outcome = outcome;
    }

    public static Bid place(Long auctionId, Long bidderId, Money amount, LocalDateTime placedAt) {
        Objects.requireNonNull(auctionId, "경매 ID는 null일 수 없습니다.");
        Objects.requireNonNull(bidderId, "입찰자 ID는 null일 수 없습니다.");
        Objects.requireNonNull(amount, "입찰 금액은 null일 수 없습니다.");
        Objects.requireNonNull(placedAt, "입찰 시각은 null일 수 없습니다.");
        return new Bid(auctionId, bidderId, amount, placedAt, BidOutcome.ACTIVE);
    }

    public void markOutbid() {
        changeOutcome(BidOutcome.OUTBID);
    }

    public void markWon() {
        changeOutcome(BidOutcome.WON);
    }

    private void changeOutcome(BidOutcome next) {
        if (!this.outcome.canTransitTo(next)) {
            throw new IllegalStateException("허용되지 않은 입찰 결과 전이입니다: %s → %s".formatted(this.outcome, next));
        }
        this.outcome = next;
    }

}
