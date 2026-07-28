package site.coreservice.pointwallet.hold.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.pointwallet.hold.domain.Hold;
import site.coreservice.pointwallet.hold.domain.HoldRepository;
import site.coreservice.pointwallet.hold.exception.HoldErrorCode;
import site.coreservice.pointwallet.hold.exception.HoldException;
import site.coreservice.pointwallet.ledger.application.PointTransactionService;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.application.WalletBalanceResult;
import site.coreservice.pointwallet.wallet.application.WalletService;
import site.coreservice.pointwallet.wallet.domain.InsufficientBalanceException;
import site.coreservice.pointwallet.wallet.exception.WalletNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldApplicationService implements HoldService {

    private final HoldRepository holdRepository;
    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;

    @Override
    @Transactional
    public HoldResult hold(Long auctionId, Long userId, Money amount) {
        // 1) 이 경매에 기존 활성 홀드가 있으면(보통 다른 유저) 먼저 해제 — 그 사람 지갑에 환원.
        Long releasedHoldId = holdRepository.findByAuctionId(auctionId)
                .map(this::releasePreviousHold)
                .orElse(null);

        // 2) 새 최고 입찰자 지갑에서 차감. 지갑 조회·차감 검증은 전부 WalletService(→Wallet) 책임이고,
        // 여기서는 그 결과로 온 예외를 Hold 컨텍스트의 비즈니스 예외로 번역만 한다.
        WalletBalanceResult result;
        try {
            result = walletService.deduct(userId, amount);
        } catch (WalletNotFoundException e) {
            throw new HoldException(HoldErrorCode.WALLET_NOT_FOUND);
        } catch (InsufficientBalanceException e) {
            throw new HoldException(HoldErrorCode.INSUFFICIENT_BALANCE);
        }

        Hold newHold = holdRepository.save(Hold.place(auctionId, userId, amount));
        pointTransactionService.record(
                result.walletId(), PointTransactionType.HOLD, amount, result.balanceAfter(), newHold.getId()
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
        }

        pointTransactionService.record(
                result.walletId(), PointTransactionType.RELEASE, previousHold.getAmount(),
                result.balanceAfter(), previousHold.getId()
        );

        holdRepository.delete(previousHold);
        return previousHold.getId();
    }

    @Override
    @Transactional
    public void release(Long auctionId) {
        holdRepository.findByAuctionId(auctionId).ifPresentOrElse(
                this::releasePreviousHold,
                () -> log.warn("해제할 홀드 없음, 스킵: auctionId={}", auctionId)
        );
    }

    @Override
    @Transactional
    public void consume(Long auctionId) {
        holdRepository.findByAuctionId(auctionId).ifPresentOrElse(
                holdRepository::delete,
                () -> log.warn("소멸시킬 홀드 없음, 스킵: auctionId={}", auctionId)
        );
    }
}