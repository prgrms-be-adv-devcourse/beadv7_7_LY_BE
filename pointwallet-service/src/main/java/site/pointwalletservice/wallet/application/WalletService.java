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

    /**
     * 지갑 두 개를 한 트랜잭션에서 같이 건드려야 할 때 쓴다.
     * userId 오름차순으로 고정해서 잠그기 때문에, 호출부가 어떤 순서로 넘기든
     * (또는 두 값이 반대로 뒤바뀐 다른 호출과 동시에 실행되든) 데드락이 나지 않는다.
     * secondUserId가 null이거나 userId와 같으면 하나만 잠근다.
     */
    void lockForUpdate(Long userId, Long secondUserId);

    /** 거래내역 조회처럼 지갑 존재 자체가 불확실한(자동 개설하면 안 되는) 읽기 전용 조회용. */
    Optional<Long> findWalletId(Long userId);

    /**
     * 회원 본인의 잔액 조회용. 지갑이 없으면 Money.zero() — "지갑 없음"과 "잔액 0원"을
     * 프론트에서 구분할 필요가 없는 조회 API(마이페이지 지갑 탭 등)에 쓴다.
     */
    Money getBalance(Long userId);
}