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
     * 대응용 멱등성 체크 — 다른 도메인(withdraw, settlement, refund 등)이 이걸로 중복 처리를 거른다.
     */
    boolean existsForRelatedId(Long relatedId, PointTransactionType type);

    /**
     * 해당 경매의 낙찰 홀드 금액을 조회한다. Hold 로우가 이미 삭제된 뒤에도(주문완료 시 consume()이
     * 하드 딜리트) 원장의 auctionId 컬럼으로 되짚을 수 있다. PointTransaction 엔티티를 그대로
     * 노출하지 않고 필요한 값(Money)만 반환해 ledger 내부 모델이 다른 도메인으로 새지 않게 한다.
     */
    Optional<Money> findLatestHoldAmountByAuctionId(Long auctionId);

    PointTransactionSearchResult findTransactions(Long userId, String rawType,
                                                  LocalDateTime from, LocalDateTime to, int page, int size);
}