package site.pointwalletservice.withdraw.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;

@Entity
@Table(
        name = "withdraw_request",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_withdraw_user_idempotency_key",
                columnNames = {"user_id", "idempotency_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Withdraw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /**
     * 유일성은 (user_id, idempotency_key) 복합으로 건다 - 키 단독 유니크였다면 다른 사용자의
     * 키 문자열을 그대로 보내는 요청이 그 사람의 인출 건을 조회/반환받는 경로가 생긴다
     * (조회 쪽에서 소유자 대조가 없었음). findByUserIdAndIdempotencyKey()도 항상 userId를
     * 함께 넘겨 대조하므로, 이 컬럼 자체는 유저 스코프 밖에서 유일할 필요가 없다.
     */
    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    @Embedded
    private Money amount;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "fee_amount"))
    private Money feeAmount;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "net_amount"))
    private Money netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WithdrawStatus status;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 64;

    private Withdraw(Long userId, String idempotencyKey, Money amount, Money feeAmount, Money netAmount) {
        validateIdempotencyKey(idempotencyKey);
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.status = WithdrawStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public static Withdraw request(Long userId, String idempotencyKey, Money amount, Money feeAmount, Money netAmount) {
        return new Withdraw(userId, idempotencyKey, amount, feeAmount, netAmount);
    }


    public void complete() {
        validateStatus(WithdrawStatus.PENDING);
        this.status = WithdrawStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        validateStatus(WithdrawStatus.PENDING);
        this.status = WithdrawStatus.FAILED;
        this.failReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    private void validateStatus(WithdrawStatus expected) {
        if (this.status != expected) {
            throw new WithdrawException(WithdrawErrorCode.ALREADY_PROCESSED);
        }
    }

    /**
     * 컨트롤러가 엔티티를 만들기 전에(즉, 계좌 조회 같은 외부 호출을 하기 전에) 미리 형식만
     * 검증할 수 있도록 정적으로 공개한다. 생성자도 같은 메서드를 호출하므로 규칙은 이 한 곳에만
     * 존재한다 - 컨트롤러를 거치지 않는 다른 호출 경로가 생겨도 이 검증은 항상 걸린다.
     */
    public static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new WithdrawException(WithdrawErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
    }
}