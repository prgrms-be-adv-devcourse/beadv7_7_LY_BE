package site.pointwalletservice.wallet.infrastructure;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import site.pointwalletservice.wallet.domain.Wallet;

public interface WalletJpaRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    /**
     * 잔액을 바꾸는 모든 경로(충전/차감/환원)는 이걸로 조회해야 함 — 조회~save 사이 다른 트랜잭션 진입 차단.
     * 홀드 행 락(HoldJpaRepository.findByAuctionIdForUpdate)과 동일하게 NOWAIT을 쓴다.
     * <p>
     * 원래는 "같은 유저의 동시 입찰도 정책상 정상이니 지갑 락은 기다려야 한다"고 보고
     * setLockWaitTimeout()으로 InnoDB 세션 타임아웃을 짧게 줄여 기다리는 방식을 썼었는데, native SQL이라
     * DB 종속성이 커지고, 대기 중에도 여전히 커넥션을 붙잡고 있어서 근본적인 해결책이 아니었다.
     * <p>
     * 지금은 그 경합("같은 유저 동시 입찰")을 여기서 기다려서 해소하지 않고, NOWAIT으로 즉시 실패시킨 뒤
     * RetryingHoldService가 hold() 호출 자체를 바깥에서 재시도하는 방식으로 흡수한다 — 실패한 시도의
     * 트랜잭션·커넥션이 완전히 반납된 뒤에 다음 시도가 시작되므로, 여러 서비스가 공유하는 MySQL 인스턴스의
     * 커넥션을 오래 붙잡지 않는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select w from Wallet w where w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}