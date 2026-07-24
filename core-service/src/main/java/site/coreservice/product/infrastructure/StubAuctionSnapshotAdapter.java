package site.coreservice.product.infrastructure;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.coreservice.product.domain.AuctionSnapshotPort;
import site.coreservice.product.domain.ClosedAuction;
import site.coreservice.product.domain.MediaCondition;
import site.coreservice.product.domain.ProductRepository;

/**
 * 아직 없는 경매 조회 API의 대역. 경매 id만으로 그럴싸한 마감 경매 정보를 만들어 돌려준다.
 * 같은 id를 넣으면 (같은 날, 같은 상품 데이터 기준) 항상 같은 응답이 나온다 — 재실행·데모 재현을 위해
 * 난수를 쓰지 않고 id에서 값을 파생시킨다. 상품 id는 실제 DB의 활성 상품 중에서 고르므로
 * 존재하지 않는 상품을 가리키는 시세가 생기지 않는다.
 * <p>
 * 90000번대는 실패 시연용 예약 대역: 90000~90499는 "경매 없음", 90500~90999는 "아직 안 마감".
 * 대량 시드는 이 대역과 겹치지 않는 1부터의 id를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class StubAuctionSnapshotAdapter implements AuctionSnapshotPort {

    static final long NOT_FOUND_FROM = 90_000L;
    static final long OPEN_FROM = 90_500L;
    static final long RESERVED_END = 91_000L;

    private final ProductRepository productRepository;

    @Override
    public Optional<ClosedAuction> findClosedAuction(Long auctionId) {
        if (auctionId >= NOT_FOUND_FROM && auctionId < OPEN_FROM) {
            return Optional.empty();
        }
        if (auctionId >= OPEN_FROM && auctionId < RESERVED_END) {
            return Optional.of(buildAuction(auctionId, "RUNNING"));
        }
        return Optional.of(buildAuction(auctionId, "ENDED_WON"));
    }

    private ClosedAuction buildAuction(Long auctionId, String status) {
        List<Long> productIds = productRepository.findAllActiveIds();
        if (productIds.isEmpty()) {
            throw new IllegalStateException("활성 상품이 없어 가짜 경매 응답을 만들 수 없습니다 — 상품 시드를 먼저 넣으세요");
        }
        long seed = auctionId;
        Long productId = productIds.get((int) (seed % productIds.size()));
        MediaCondition condition = MediaCondition.values()[(int) (seed % MediaCondition.values().length)];
        long finalPrice = 30_000L + (seed * 7919 % 70) * 1_000L;
        int bidCount = (int) (seed * 31 % 15) + 1;
        LocalDateTime closedAt = LocalDate.now().atTime(20, 31).minusDays(seed * 13 % 365 + 1);
        return new ClosedAuction(auctionId, productId, condition, finalPrice, bidCount, closedAt, status);
    }
}
