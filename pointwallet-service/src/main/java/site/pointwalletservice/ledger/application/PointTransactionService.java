package site.pointwalletservice.ledger.application;
import java.time.LocalDateTime;
import java.util.Optional;
import site.pointwalletservice.ledger.application.dto.PointTransactionSearchResult;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;

public interface PointTransactionService {

    void record(Long walletId, PointTransactionType type, Money amount, Money balanceAfter, Long relatedId);

    /** HOLD/RELEASE 전용 — auctionId까지 원장에 남겨서, Hold 로우가 삭제된 뒤에도 경매 기준으로 조회 가능하게 한다. */
    void recordForAuction(Long walletId, PointTransactionType type, Money amount, Money balanceAfter,
                          Long relatedId, Long auctionId);

    /**
     * 같은 근원(relatedId)에 대해 이 타입의 원장이 이미 있는지 확인한다. Kafka at-least-once 재전달
     * 대응용 멱등성 체크 — 다른 도메인(withdraw, settlement 등)이 이걸로 중복 처리를 거른다.
     */
    boolean existsForRelatedId(Long relatedId, PointTransactionType type);

    /** holdId(=related_id)로 HOLD 원장의 auctionId를 조회 — Hold 롤백(보상 트랜잭션)이 holdId만 갖고도 대상 경매를 재구성할 수 있게 한다. */
    Optional<Long> findAuctionIdByHoldId(Long holdId);

    PointTransactionSearchResult findTransactions(Long userId, String rawType,
                                                  LocalDateTime from, LocalDateTime to, int page, int size);
}