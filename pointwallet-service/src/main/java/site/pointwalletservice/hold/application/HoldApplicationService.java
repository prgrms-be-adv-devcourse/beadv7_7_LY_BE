package site.pointwalletservice.hold.application;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.hold.domain.Hold;
import site.pointwalletservice.hold.domain.HoldRepository;
import site.pointwalletservice.hold.exception.HoldErrorCode;
import site.pointwalletservice.hold.exception.HoldException;
import site.pointwalletservice.hold.exception.HoldLockContentionException;
import site.pointwalletservice.hold.exception.HoldRowLockContentionException;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletLockFailedException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldApplicationService implements HoldService {

    private final HoldRepository holdRepository;
    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;

    /**
     * NOWAIT 락 조회 시 이미 다른 트랜잭션이 이 auction의 Hold를 잠그고 있으면 Spring이
     * PessimisticLockingFailureException으로 던진다 - 그대로 두면 GlobalExceptionHandler의
     * catch-all(GERR-0001)로 뭉개지니, 여기서 Hold 컨텍스트의 에러코드로 번역한다.
     * <p>
     * HoldLockContentionException(지갑 락 경합)이 아니라 HoldRowLockContentionException을 던진다 -
     * 이건 auction-service가 즉시 알아야 하는 신호라 RetryingHoldService가 재시도하지 않는다
     * (HoldRowLockContentionException 클래스 주석 참고).
     */
    private Optional<Hold> findByAuctionIdForUpdate(Long auctionId) {
        try {
            return holdRepository.findByAuctionIdForUpdate(auctionId);
        } catch (PessimisticLockingFailureException e) {
            throw new HoldRowLockContentionException();
        }
    }

    @Override
    @Transactional
    public HoldResult hold(Long auctionId, Long userId, Money amount) {
        // 이 경매의 Hold 행 자체를 먼저 잠근다 — 같은 auctionId에 대한 동시 hold() 호출을
        // 여기서 직렬화시켜서, 아래에서 읽는 previousUserId가 이 트랜잭션이 끝날 때까지
        // 더 이상 바뀌지 않는다는 걸 보장한다. (지갑 락만으로는 auction_id 행 자체의 변경을
        // 못 막아서, previousUserId를 읽은 시점과 실제 해제 시점 사이에 값이 바뀌는
        // TOCTOU 레이스가 있었음 - PR 리뷰 참고)
        Optional<Hold> currentHold = findByAuctionIdForUpdate(auctionId);
        Long previousUserId = currentHold.map(Hold::getUserId).orElse(null);

        // 데드락 방지: 지갑 두 개를 건드릴 수 있으니, WalletService가 정해둔 고정 순서로 잠근다.
        try {
            walletService.lockForUpdate(userId, previousUserId);
        } catch (WalletLockFailedException e) {
            throw new HoldLockContentionException();
        }

        // 1) 이 경매에 기존 활성 홀드가 있으면(보통 다른 유저) 먼저 해제 — 그 사람 지갑에 환원.
        // 위에서 이미 락을 잡고 읽어둔 currentHold를 그대로 쓴다(재조회 불필요 - 더 이상 안 바뀜).
        Long releasedHoldId = currentHold
                .map(this::releasePreviousHold)
                .orElse(null);
        // 2) 새 최고 입찰자 지갑에서 차감. 지갑 조회·차감 검증은 전부 WalletService(→Wallet) 책임이고,
        // 여기서는 그 결과로 온 예외를 Hold 컨텍스트의 비즈니스 예외로 번역만 한다.
        WalletBalanceResult result;
        try {
            result = walletService.deduct(userId, amount);
        } catch (WalletNotFoundException e) {
            throw new HoldException(HoldErrorCode.INSUFFICIENT_BALANCE);
        } catch (InsufficientBalanceException e) {
            throw new HoldException(HoldErrorCode.INSUFFICIENT_BALANCE);
        }

        Hold newHold = holdRepository.save(Hold.place(auctionId, userId, amount));
        pointTransactionService.recordForAuction(
                result.walletId(), PointTransactionType.HOLD, amount, result.balanceAfter(), newHold.getId(), auctionId
        );

        return new HoldResult(newHold.getId(), releasedHoldId, result.balanceAfter());
    }

    /** 이전 최고 입찰자(새 입찰자와 다른 유저일 수 있음)의 홀드를 해제한다: 그 사람 지갑에 환원 + 원장 기록 + 홀드 레코드 삭제. */
    private Long releasePreviousHold(Hold previousHold) {
        // credit()을 쓴다 - charge()와 달리 지갑이 없으면 자동 개설하지 않고 예외를 던진다.
        // 이전 입찰자는 홀드를 걸었던 시점에 이미 지갑이 있었어야 하므로, 없다면 데이터 정합성 문제이지
        // "신규 유저라 지갑이 없는" 정상 케이스가 아니다 - 조용히 새 지갑을 만들어 덮으면 안 된다.
        WalletBalanceResult result;
        try {
            result = walletService.credit(previousHold.getUserId(), previousHold.getAmount());
        } catch (WalletNotFoundException e) {
            throw new HoldException(HoldErrorCode.WALLET_NOT_FOUND);
        } catch (WalletLockFailedException e) {
            throw new HoldLockContentionException();
        }

        pointTransactionService.recordForAuction(
                result.walletId(), PointTransactionType.RELEASE, previousHold.getAmount(),
                result.balanceAfter(), previousHold.getId(), previousHold.getAuctionId()
        );

        holdRepository.delete(previousHold);
        return previousHold.getId();
    }

    @Override
    @Transactional
    public void release(Long auctionId) {
        // hold()와 같은 이유로 잠긴 조회를 쓴다 — 동시에 들어온 hold()가 같은 auction의
        // Hold를 바꾸는 도중과 겹치지 않게 직렬화한다.
        findByAuctionIdForUpdate(auctionId).ifPresentOrElse(
                this::releasePreviousHold,
                () -> log.warn("해제할 홀드 없음, 스킵: auctionId={}", auctionId)
        );
    }

    @Override
    @Transactional
    public void consume(Long auctionId) {
        findByAuctionIdForUpdate(auctionId).ifPresentOrElse(
                holdRepository::delete,
                () -> log.warn("소멸시킬 홀드 없음, 스킵: auctionId={}", auctionId)
        );
    }
}