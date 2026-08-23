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

    @Override
    @Transactional
    public void rollback(Long holdId, Long auctionId, Long userId, Money amount) {
        // 1) 멱등성 체크 — 이 holdId가 이미 되돌려졌으면(자연 교체로 releasePreviousHold가 이미 RELEASE를
        // 남겼거나, auction-service의 재시도로 이 API가 이미 한 번 성공했으면) 조용히 스킵한다.
        // 자금은 이미 안전한 상태라 여기서 요청값(auctionId/userId/amount)을 검증할 필요는 없지만,
        // 그렇다고 검증을 아예 건너뛰면 "이 holdId, 사실 저 auctionId 거 아닌데?" 같은 호출자 쪽
        // 버그가 조용히 묻힌다 — 차단하지 않되(자금엔 영향 없으니) 불일치가 보이면 로그는 남긴다.
        if (pointTransactionService.existsForRelatedId(holdId, PointTransactionType.RELEASE)) {
            pointTransactionService.findAuctionIdByHoldId(holdId)
                    .filter(ledgerAuctionId -> !ledgerAuctionId.equals(auctionId))
                    .ifPresent(ledgerAuctionId -> log.warn(
                            "이미 롤백된 홀드지만 요청의 auctionId가 원장과 불일치 - 호출자 쪽 확인 필요: holdId={}, 요청auctionId={}, 원장auctionId={}",
                            holdId, auctionId, ledgerAuctionId));
            log.info("이미 롤백된 홀드, 스킵: holdId={}", holdId);
            return;
        }

        // 2) 원장에서 이 holdId가 걸렸던 auctionId를 되짚는다. Hold 행이 이미 삭제됐어도(교체/소멸)
        // 원장(HOLD 타입, related_id=holdId)만으로 재구성 가능 — auction_id 컬럼을 이 목적으로 추가해뒀다.
        Long ledgerAuctionId = pointTransactionService.findAuctionIdByHoldId(holdId)
                .orElseThrow(() -> new HoldException(HoldErrorCode.HOLD_NOT_FOUND));

        // 3) 최소 검증 — 요청의 auctionId가 원장 기록과 다르면, 더 뒤져서 맞춰보지 않고 그 자리에서 거부한다.
        // 원장은 append-only라 (auctionId, userId, amount)만으로 "지금 유효한 그 홀드"를 유일하게
        // 특정할 방법이 없다 - 추측 매칭은 잘못된 대상을 건드릴 위험만 키운다.
        if (!ledgerAuctionId.equals(auctionId)) {
            log.error("롤백 요청의 auctionId가 원장과 불일치: holdId={}, 요청={}, 원장={}", holdId, auctionId, ledgerAuctionId);
            throw new HoldException(HoldErrorCode.HOLD_MISMATCH);
        }

        // 4) auctionId 기준으로 잠그고, 지금 걸려있는 홀드가 정확히 이 holdId인지 확인한다.
        // release(auctionId)와 다른 지점이 바로 이거다 — "지금 걸려있는 걸 무조건" 푸는 게 아니라
        // "내가 되돌리려는 그 holdId가 맞는지" 확인 후에만 푼다. 1번에서 RELEASE가 없었는데도
        // 여기서 비어있거나 다른 holdId면, 정상 흐름(교체 시 항상 RELEASE를 먼저 남김)과 어긋나는
        // 상태라 원장-Hold 간 불일치로 보고 즉시 실패시킨다 — 조용히 아무 hold나 건드리면 안 된다.
        Hold currentHold = findByAuctionIdForUpdate(ledgerAuctionId)
                .filter(hold -> hold.getId().equals(holdId))
                .orElseThrow(() -> {
                    log.error("롤백 대상 홀드가 원장과 불일치 — 이미 교체/소멸된 것으로 추정: holdId={}, auctionId={}", holdId, ledgerAuctionId);
                    return new HoldException(HoldErrorCode.HOLD_ALREADY_FINALIZED);
                });

        // 5) 남은 최소 검증 — userId/amount도 요청값과 실제 Hold가 일치하는지 확인한다.
        // 여기서부터는 이미 락을 잡고 조회해둔 currentHold로 비교하므로 원장을 추가로 읽을 필요가 없다.
        if (!currentHold.getUserId().equals(userId) || !currentHold.getAmount().equals(amount)) {
            log.error("롤백 요청의 userId/amount가 실제 홀드와 불일치: holdId={}, 요청=(userId={}, amount={}), 실제=(userId={}, amount={})",
                    holdId, userId, amount, currentHold.getUserId(), currentHold.getAmount());
            throw new HoldException(HoldErrorCode.HOLD_MISMATCH);
        }

        // 6) 실제 되돌리기 — hold() 안에서 이전 홀드를 해제할 때 쓰는 것과 동일한 로직 재사용
        // (지갑 환급 + RELEASE 원장 기록 + Hold 행 삭제).
        releasePreviousHold(currentHold);
    }
}