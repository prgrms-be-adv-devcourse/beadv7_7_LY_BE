package site.coreservice.pointwallet.hold.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.pointwallet.hold.domain.Hold;
import site.coreservice.pointwallet.hold.domain.HoldRepository;
import site.coreservice.pointwallet.hold.exception.HoldErrorCode;
import site.coreservice.pointwallet.hold.exception.HoldException;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.domain.InsufficientBalanceException;
import site.coreservice.pointwallet.wallet.domain.Wallet;
import site.coreservice.pointwallet.wallet.domain.WalletRepository;

@Service
@RequiredArgsConstructor
public class HoldApplicationService implements HoldService {

    private final HoldRepository holdRepository;
    private final WalletRepository walletRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Override
    @Transactional
    public HoldResult hold(Long auctionId, Long userId, Money amount) {
        // 1) 이 경매에 기존 활성 홀드가 있으면(보통 다른 유저) 먼저 해제 — 그 사람 지갑에 환원.
        Long releasedHoldId = holdRepository.findByAuctionId(auctionId)
                .map(this::releasePreviousHold)
                .orElse(null);

        // 2) 새 최고 입찰자 지갑에서 차감하고 새 홀드를 생성.
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new HoldException(HoldErrorCode.WALLET_NOT_FOUND));

        try {
            wallet.deduct(amount);
        } catch (InsufficientBalanceException e) {
            throw new HoldException(HoldErrorCode.INSUFFICIENT_BALANCE);
        }
        walletRepository.save(wallet);

        Hold newHold = holdRepository.save(Hold.place(auctionId, userId, amount));
        recordTransaction(wallet, PointTransactionType.HOLD, amount, newHold.getId());

        return new HoldResult(newHold.getId(), releasedHoldId, wallet.getBalance());
    }

    /** 이전 최고 입찰자(새 입찰자와 다른 유저일 수 있음)의 홀드를 해제한다: 그 사람 지갑에 환원 + 원장 기록 + 홀드 레코드 삭제. */
    private Long releasePreviousHold(Hold previousHold) {
        Wallet previousWallet = walletRepository.findByUserId(previousHold.getUserId())
                .orElseThrow(() -> new HoldException(HoldErrorCode.WALLET_NOT_FOUND));

        previousWallet.charge(previousHold.getAmount());
        walletRepository.save(previousWallet);
        recordTransaction(previousWallet, PointTransactionType.RELEASE, previousHold.getAmount(), previousHold.getId());

        holdRepository.delete(previousHold);
        return previousHold.getId();
    }

    private void recordTransaction(Wallet wallet, PointTransactionType type, Money amount, Long relatedId) {
        PointTransaction transaction = PointTransaction.record(
                wallet.getId(), type, amount, wallet.getBalance(), relatedId
        );
        pointTransactionRepository.save(transaction);
    }
}