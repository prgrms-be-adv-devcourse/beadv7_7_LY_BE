package site.pointwalletservice.withdraw.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;


class WithdrawTest {

    private static final Money AMOUNT = Money.of(100_000);
    private static final Money FEE = Money.of(2_000);
    private static final Money NET = Money.of(98_000);
    private static final String IDEMPOTENCY_KEY = "test-idem-key-0001";

    @Test
    @DisplayName("request()로 만들면 PENDING 상태로 시작한다")
    void request_초기상태_PENDING() {
        // when
        Withdraw withdraw = Withdraw.request(1L, IDEMPOTENCY_KEY, AMOUNT, FEE, NET);

        // then
        assertThat(withdraw.getStatus()).isEqualTo(WithdrawStatus.PENDING);
        assertThat(withdraw.getProcessedAt()).isNull();
        assertThat(withdraw.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("complete()하면 SUCCESS로 바뀌고 processedAt이 채워진다")
    void complete_SUCCESS로_전이() {
        // given
        Withdraw withdraw = Withdraw.request(1L, IDEMPOTENCY_KEY, AMOUNT, FEE, NET);

        // when
        withdraw.complete();

        // then
        assertThat(withdraw.getStatus()).isEqualTo(WithdrawStatus.SUCCESS);
        assertThat(withdraw.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("fail()하면 FAILED로 바뀌고 사유가 남는다")
    void fail_FAILED로_전이() {
        // given
        Withdraw withdraw = Withdraw.request(1L, IDEMPOTENCY_KEY, AMOUNT, FEE, NET);

        // when
        withdraw.fail("계좌 확인 실패");

        // then
        assertThat(withdraw.getStatus()).isEqualTo(WithdrawStatus.FAILED);
        assertThat(withdraw.getFailReason()).isEqualTo("계좌 확인 실패");
        assertThat(withdraw.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 처리된(PENDING이 아닌) 건을 다시 처리하려 하면 ALREADY_PROCESSED를 던진다")
    void 이미처리된건_재처리시_예외() {
        // given
        Withdraw withdraw = Withdraw.request(1L, IDEMPOTENCY_KEY, AMOUNT, FEE, NET);
        withdraw.complete();

        // when & then
        assertThatThrownBy(withdraw::complete)
                .isInstanceOf(WithdrawException.class)
                .extracting(e -> ((WithdrawException) e).getErrorCode())
                .isEqualTo(WithdrawErrorCode.ALREADY_PROCESSED);

        assertThatThrownBy(() -> withdraw.fail("사유"))
                .isInstanceOf(WithdrawException.class)
                .extracting(e -> ((WithdrawException) e).getErrorCode())
                .isEqualTo(WithdrawErrorCode.ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("멱등키가 null이면 request()에서 IDEMPOTENCY_KEY_REQUIRED를 던진다")
    void 멱등키_null이면_예외() {
        assertThatThrownBy(() -> Withdraw.request(1L, null, AMOUNT, FEE, NET))
                .isInstanceOf(WithdrawException.class)
                .extracting(e -> ((WithdrawException) e).getErrorCode())
                .isEqualTo(WithdrawErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    @DisplayName("멱등키가 공백(blank)이면 request()에서 IDEMPOTENCY_KEY_REQUIRED를 던진다")
    void 멱등키_blank이면_예외() {
        assertThatThrownBy(() -> Withdraw.request(1L, "   ", AMOUNT, FEE, NET))
                .isInstanceOf(WithdrawException.class)
                .extracting(e -> ((WithdrawException) e).getErrorCode())
                .isEqualTo(WithdrawErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    @DisplayName("멱등키가 64자를 초과하면 request()에서 IDEMPOTENCY_KEY_REQUIRED를 던진다")
    void 멱등키_길이초과면_예외() {
        String tooLong = "a".repeat(Withdraw.IDEMPOTENCY_KEY_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Withdraw.request(1L, tooLong, AMOUNT, FEE, NET))
                .isInstanceOf(WithdrawException.class)
                .extracting(e -> ((WithdrawException) e).getErrorCode())
                .isEqualTo(WithdrawErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    @DisplayName("멱등키가 정확히 64자면 통과한다 (경계값)")
    void 멱등키_정확히_64자면_통과() {
        String exactly64 = "a".repeat(Withdraw.IDEMPOTENCY_KEY_MAX_LENGTH);

        Withdraw withdraw = Withdraw.request(1L, exactly64, AMOUNT, FEE, NET);

        assertThat(withdraw.getIdempotencyKey()).hasSize(64);
    }

    @Test
    @DisplayName("validateIdempotencyKey()는 컨트롤러가 엔티티 생성 없이 같은 규칙을 미리 확인할 수 있게 정적으로 공개된다")
    void validateIdempotencyKey_정적으로_동일규칙_적용() {
        assertThatThrownBy(() -> Withdraw.validateIdempotencyKey(""))
                .isInstanceOf(WithdrawException.class)
                .extracting(e -> ((WithdrawException) e).getErrorCode())
                .isEqualTo(WithdrawErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }
}