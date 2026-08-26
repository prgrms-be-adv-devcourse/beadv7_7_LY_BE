package site.pointwalletservice.wallet.deadletter.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import site.pointwalletservice.wallet.application.WithdrawFeeEarnedEventHandler;
import site.pointwalletservice.wallet.deadletter.domain.DeadLetterStatus;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetter;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetterRepository;
import site.pointwalletservice.wallet.exception.WithdrawFeeDeadLetterNotFoundException;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawFeeDeadLetterAdminService")
class WithdrawFeeDeadLetterAdminServiceTest {

    @Mock
    private WithdrawFeeDeadLetterRepository repository;

    @Mock
    private WithdrawFeeEarnedEventHandler withdrawFeeEarnedEventHandler;

    private WithdrawFeeDeadLetterAdminService sut;

    @BeforeEach
    void setUp() {
        sut = new WithdrawFeeDeadLetterAdminService(repository, withdrawFeeEarnedEventHandler);
    }

    @Test
    @DisplayName("findByStatus는 리포지토리의 상태별 최신순 조회를 그대로 위임한다")
    void findByStatus_리포지토리에_위임한다() {
        WithdrawFeeDeadLetter deadLetter =
                WithdrawFeeDeadLetter.open(1L, BigDecimal.valueOf(2_000), "실패");
        when(repository.findByStatusOrderByCreatedAtDesc(DeadLetterStatus.OPEN))
                .thenReturn(List.of(deadLetter));

        List<WithdrawFeeDeadLetter> result = sut.findByStatus(DeadLetterStatus.OPEN);

        assertThat(result).containsExactly(deadLetter);
    }

    @Test
    @DisplayName("reprocess는 원본 이벤트를 복원해 핸들러를 재호출하고 성공하면 RESOLVED로 전환한다")
    void reprocess_핸들러를_재호출하고_RESOLVED로_전환한다() {
        WithdrawFeeDeadLetter deadLetter =
                WithdrawFeeDeadLetter.open(5L, BigDecimal.valueOf(3_000), "일시적 DB 오류");
        when(repository.findById(1L)).thenReturn(Optional.of(deadLetter));

        sut.reprocess(1L);

        verify(withdrawFeeEarnedEventHandler)
                .handle(new WithdrawFeeEarnedEvent(5L, BigDecimal.valueOf(3_000)));
        assertThat(deadLetter.getStatus()).isEqualTo(DeadLetterStatus.RESOLVED);
    }

    @Test
    @DisplayName("reprocess 대상이 존재하지 않으면 예외가 발생하고 핸들러는 호출되지 않는다")
    void reprocess_존재하지_않으면_예외() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.reprocess(999L))
                .isInstanceOf(WithdrawFeeDeadLetterNotFoundException.class);
        verify(withdrawFeeEarnedEventHandler, never()).handle(any(WithdrawFeeEarnedEvent.class));
    }

    @Test
    @DisplayName("resolve는 핸들러를 호출하지 않고 사유만 남긴 채 RESOLVED로 전환한다")
    void resolve_핸들러_호출없이_RESOLVED로_전환한다() {
        WithdrawFeeDeadLetter deadLetter =
                WithdrawFeeDeadLetter.open(5L, BigDecimal.valueOf(3_000), "일시적 DB 오류");
        when(repository.findById(1L)).thenReturn(Optional.of(deadLetter));

        sut.resolve(1L, "이미 별도로 정산 처리함");

        verify(withdrawFeeEarnedEventHandler, never()).handle(any(WithdrawFeeEarnedEvent.class));
        assertThat(deadLetter.getStatus()).isEqualTo(DeadLetterStatus.RESOLVED);
        assertThat(deadLetter.getResolvedNote()).isEqualTo("이미 별도로 정산 처리함");
    }

    @Test
    @DisplayName("reprocess 중 유니크 제약 위반이 나면 실패가 아니라 이미 처리된 것으로 간주해 RESOLVED로 전환한다")
    void reprocess_유니크제약위반시_이미처리된것으로_간주하고_RESOLVED로_전환한다() {
        WithdrawFeeDeadLetter deadLetter =
                WithdrawFeeDeadLetter.open(5L, BigDecimal.valueOf(3_000), "일시적 DB 오류");
        when(repository.findById(1L)).thenReturn(Optional.of(deadLetter));
        doThrow(new DataIntegrityViolationException("uk_point_transaction_related_id_type"))
                .when(withdrawFeeEarnedEventHandler)
                .handle(new WithdrawFeeEarnedEvent(5L, BigDecimal.valueOf(3_000)));

        sut.reprocess(1L);

        assertThat(deadLetter.getStatus()).isEqualTo(DeadLetterStatus.RESOLVED);
        assertThat(deadLetter.getResolvedNote()).contains("이미 처리된 이벤트");
    }

    @Test
    @DisplayName("resolve 대상이 존재하지 않으면 예외가 발생한다")
    void resolve_존재하지_않으면_예외() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.resolve(999L, "메모"))
                .isInstanceOf(WithdrawFeeDeadLetterNotFoundException.class);
    }
}