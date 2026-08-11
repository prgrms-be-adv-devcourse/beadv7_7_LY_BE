package site.pointwalletservice.wallet.infrastructure;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.pointwalletservice.wallet.domain.Wallet;

/**
 * 지갑 락을 NOWAIT으로 바꾸기 전, "InnoDB 세션 타임아웃을 5초로 줄이고 기다리는" 옛날 방식을
 * 재현한 테스트 전용 레포지토리. 프로덕션 코드(WalletJpaRepository)에는 이제 없는 메서드라,
 * NOWAIT+재시도 방식과의 성능 비교를 위해서만 여기 남겨둔다.
 */
public interface LegacyWaitWalletRepository extends JpaRepository<Wallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdateWait(@Param("userId") Long userId);

    @Modifying
    @Query(value = "SET innodb_lock_wait_timeout = :seconds", nativeQuery = true)
    void setLockWaitTimeout(@Param("seconds") int seconds);
}