package site.pointwalletservice.ledger.domain;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PointTransactionRepository {
    PointTransaction save(PointTransaction pointTransaction);
    PointTransactionSearchPage search(Long walletId, PointTransactionType type,
                                      LocalDateTime from, LocalDateTime to, int page, int size);
    boolean existsByRelatedIdAndType(Long relatedId, PointTransactionType type); // 추가

    /**
     * 해당 경매의 낙찰 홀드 원장을 조회한다. 재입찰 시마다 이전 홀드는 RELEASE 처리되고 사라지므로,
     * auctionId+HOLD로 남아있는 마지막 한 건이 곧 낙찰 건이다. 환불 등 Hold 로우가 이미 삭제된
     * 시점에도 원장만으로 금액을 되짚어 찾기 위해 쓴다.
     */
    Optional<PointTransaction> findLatestByAuctionIdAndType(Long auctionId, PointTransactionType type);
}