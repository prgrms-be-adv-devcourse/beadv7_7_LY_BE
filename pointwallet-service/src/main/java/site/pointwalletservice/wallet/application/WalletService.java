package site.pointwalletservice.wallet.application;
import java.util.Optional;
import site.pointwalletservice.shared.Money;


public interface WalletService {

    WalletBalanceResult charge(Long userId, Money amount);

    WalletBalanceResult credit(Long userId, Money amount);

    WalletBalanceResult deduct(Long userId, Money amount);

    /**
     * 지갑이 존재하면 비관적 락만 미리 걸어둔다 (값은 안 씀).
     * 여러 지갑을 한 트랜잭션에서 건드릴 때, 데드락을 피하려고 정해진 순서로
     * 먼저 락을 잡아두기 위한 용도. 지갑이 없으면 조용히 스킵한다 —
     * 이후 실제 charge/credit/deduct에서 지갑 없음을 정상적으로 처리한다.
     */
    void lockForUpdate(Long userId);

    /** 거래내역 조회처럼 지갑 존재 자체가 불확실한(자동 개설하면 안 되는) 읽기 전용 조회용. */
    Optional<Long> findWalletId(Long userId);

    /**
     * 회원 본인의 잔액 조회용. 지갑이 없으면 Money.zero() — "지갑 없음"과 "잔액 0원"을
     * 프론트에서 구분할 필요가 없는 조회 API(마이페이지 지갑 탭 등)에 쓴다.
     */
    Money getBalance(Long userId);
}