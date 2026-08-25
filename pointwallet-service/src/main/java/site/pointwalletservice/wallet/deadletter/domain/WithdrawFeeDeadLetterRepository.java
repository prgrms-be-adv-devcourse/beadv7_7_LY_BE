package site.pointwalletservice.wallet.deadletter.domain;
import java.util.List;
import java.util.Optional;

public interface WithdrawFeeDeadLetterRepository {

    WithdrawFeeDeadLetter save(WithdrawFeeDeadLetter deadLetter);

    Optional<WithdrawFeeDeadLetter> findById(Long id);

    /** 관리자 목록 조회용 - 최신순. */
    List<WithdrawFeeDeadLetter> findByStatusOrderByCreatedAtDesc(DeadLetterStatus status);
}