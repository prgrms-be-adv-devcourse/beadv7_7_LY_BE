package site.pointwalletservice.wallet.deadletter.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.pointwalletservice.wallet.deadletter.domain.DeadLetterStatus;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetter;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetterRepository;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawFeeDeadLetterListener")
class WithdrawFeeDeadLetterListenerTest {

    @Mock
    private WithdrawFeeDeadLetterRepository repository;

    private WithdrawFeeDeadLetterListener sut;

    @BeforeEach
    void setUp() {
        sut = new WithdrawFeeDeadLetterListener(repository);
    }

    @Test
    @DisplayName("DLT 레코드를 받으면 이벤트 값과 실패 원인을 담아 OPEN 상태로 저장한다")
    void handle_DLT레코드를_OPEN상태로_저장한다() {
        WithdrawFeeEarnedEvent event = new WithdrawFeeEarnedEvent(7L, BigDecimal.valueOf(1_500));

        sut.handle(event, "커넥션 타임아웃");

        ArgumentCaptor<WithdrawFeeDeadLetter> captor = ArgumentCaptor.forClass(WithdrawFeeDeadLetter.class);
        verify(repository).save(captor.capture());
        WithdrawFeeDeadLetter saved = captor.getValue();
        assertThat(saved.getWithdrawId()).isEqualTo(7L);
        assertThat(saved.getFeeAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_500));
        assertThat(saved.getCauseMessage()).isEqualTo("커넥션 타임아웃");
        assertThat(saved.getStatus()).isEqualTo(DeadLetterStatus.OPEN);
    }

    @Test
    @DisplayName("실패 원인 헤더가 없어도(null) 저장 자체는 정상 처리된다")
    void handle_원인헤더가_없어도_저장된다() {
        WithdrawFeeEarnedEvent event = new WithdrawFeeEarnedEvent(7L, BigDecimal.valueOf(1_500));

        sut.handle(event, null);

        verify(repository).save(any(WithdrawFeeDeadLetter.class));
    }
}