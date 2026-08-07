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

    @Test
    @DisplayName("request()로 만들면 PENDING 상태로 시작한다")
    void request_초기상태_PENDING() {
        // when
        Withdraw withdraw = Withdraw.request(1L, AMOUNT, FEE, NET);

        // then
        assertThat(withdraw.getStatus()).isEqualTo(WithdrawStatus.PENDING);
        assertThat(withdraw.getProcessedAt()).isNull();
    }

    @Test
    @DisplayName("complete()하면 SUCCESS로 바뀌고 processedAt이 채워진다")
    void complete_SUCCESS로_전이() {
        // given
        Withdraw withdraw = Withdraw.request(1L, AMOUNT, FEE, NET);

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
        Withdraw withdraw = Withdraw.request(1L, AMOUNT, FEE, NET);

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
        Withdraw withdraw = Withdraw.request(1L, AMOUNT, FEE, NET);
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
}