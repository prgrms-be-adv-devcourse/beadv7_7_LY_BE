package site.pointwalletservice.wallet.deadletter.domain;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WithdrawFeeDeadLetter")
class WithdrawFeeDeadLetterTest {

    @Test
    @DisplayName("open()으로 생성하면 OPEN 상태로 시작하고 원본 이벤트 값을 그대로 들고 있다")
    void open_OPEN상태로_생성된다() {
        WithdrawFeeDeadLetter deadLetter =
                WithdrawFeeDeadLetter.open(1L, BigDecimal.valueOf(2_000), "커넥션 타임아웃");

        assertThat(deadLetter.getWithdrawId()).isEqualTo(1L);
        assertThat(deadLetter.getFeeAmount()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
        assertThat(deadLetter.getCauseMessage()).isEqualTo("커넥션 타임아웃");
        assertThat(deadLetter.getStatus()).isEqualTo(DeadLetterStatus.OPEN);
        assertThat(deadLetter.getCreatedAt()).isNotNull();
        assertThat(deadLetter.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("resolve()를 호출하면 RESOLVED로 전환되고 사유와 처리시각이 남는다")
    void resolve_RESOLVED로_전환되고_사유가_남는다() {
        WithdrawFeeDeadLetter deadLetter =
                WithdrawFeeDeadLetter.open(1L, BigDecimal.valueOf(2_000), "커넥션 타임아웃");

        deadLetter.resolve("관리자 재처리 성공");

        assertThat(deadLetter.getStatus()).isEqualTo(DeadLetterStatus.RESOLVED);
        assertThat(deadLetter.getResolvedNote()).isEqualTo("관리자 재처리 성공");
        assertThat(deadLetter.getResolvedAt()).isNotNull();
    }
}