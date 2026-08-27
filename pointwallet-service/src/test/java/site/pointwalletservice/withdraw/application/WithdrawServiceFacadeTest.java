package site.pointwalletservice.withdraw.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryException;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.domain.WithdrawStatus;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;
import site.pointwalletservice.withdraw.exception.WithdrawLockContentionException;

@DisplayName("WithdrawServiceFacade")
class WithdrawServiceFacadeTest {

    private final RetryingWithdrawService retryingWithdrawService = mock(RetryingWithdrawService.class);
    private final WithdrawApplicationService withdrawApplicationService = mock(WithdrawApplicationService.class);
    private final WithdrawServiceFacade sut =
            new WithdrawServiceFacade(retryingWithdrawService, withdrawApplicationService);

    private static final Long USER_ID = 1L;
    private static final Money AMOUNT = Money.of(100_000);
    private static final String IDEMPOTENCY_KEY = "idem-key-1";

    @BeforeEach
    void setUp() {
        // 기본값: 아직 처리된 적 없는 새 요청 — 개별 테스트에서 다르게 스텁하지 않는 한 이걸로 통과.
        org.mockito.Mockito.lenient()
                .when(withdrawApplicationService.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("멱등키로 이미 처리된 요청을 찾으면, 계좌 검증도 재시도 서비스 호출도 없이 그 결과를 그대로 반환한다")
    void 이미처리된_멱등키면_기존결과를_그대로_반환한다() {
        // given
        WithdrawRequestResult existing = new WithdrawRequestResult(
                1L, WithdrawStatus.SUCCESS, BigDecimal.valueOf(2_000), BigDecimal.valueOf(98_000));
        when(withdrawApplicationService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existing));

        // when
        WithdrawRequestResult result = sut.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY);

        // then
        assertThat(result).isEqualTo(existing);
        verify(withdrawApplicationService, never()).validateBankAccount(USER_ID);
        verify(retryingWithdrawService, never()).requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("멱등키로 처리된 요청이 없으면 계좌 검증을 먼저 1회 실행한 뒤 재시도 서비스로 위임한다 — " +
            "재시도가 몇 번을 돌든 계좌 조회 API는 이 1회로 끝난다")
    void 신규요청이면_계좌검증_1회_후_재시도서비스로_위임한다() {
        // given
        WithdrawRequestResult expected = new WithdrawRequestResult(
                1L, WithdrawStatus.SUCCESS, BigDecimal.valueOf(2_000), BigDecimal.valueOf(98_000));
        when(retryingWithdrawService.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY)).thenReturn(expected);

        // when
        WithdrawRequestResult result = sut.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY);

        // then
        assertThat(result).isEqualTo(expected);
        verify(withdrawApplicationService).validateBankAccount(USER_ID);
        verify(retryingWithdrawService).requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("계좌 검증에서 실패하면(계좌 없음) 재시도 서비스는 아예 호출되지 않는다")
    void 계좌검증_실패시_재시도서비스_미호출() {
        // given
        WithdrawException bankAccountNotFound = new WithdrawException(WithdrawErrorCode.BANK_ACCOUNT_NOT_FOUND);
        org.mockito.Mockito.doThrow(bankAccountNotFound)
                .when(withdrawApplicationService).validateBankAccount(USER_ID);

        // when & then
        assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
                .isSameAs(bankAccountNotFound);

        verify(retryingWithdrawService, never()).requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("재시도 소진으로 UndeclaredThrowableException(cause=RetryException(cause=WithdrawLockContentionException))이 " +
            "오면, 원래 도메인 예외까지 두 겹 풀어서 그대로 던진다")
    void 재시도소진시_예외를_풀어서_원래_도메인예외를_던진다() {
        // given
        WithdrawLockContentionException domainCause = new WithdrawLockContentionException();
        RetryException retryException = new RetryException("재시도 소진", domainCause);
        when(retryingWithdrawService.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
                .thenThrow(new UndeclaredThrowableException(retryException));

        // when & then
        assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
                .isSameAs(domainCause)
                .isInstanceOf(WithdrawException.class)
                .extracting(e -> ((WithdrawException) e).getErrorCode())
                .isEqualTo(WithdrawErrorCode.LOCK_ACQUISITION_FAILED);
    }

    @Test
    @DisplayName("경합과 무관한 예외(잔액부족 등)는 재시도 자체가 안 걸려서 UndeclaredThrowableException 없이 그대로 전파된다")
    void 경합과_무관한_예외는_그대로_전파된다() {
        // given
        WithdrawException notRetried = new WithdrawException(WithdrawErrorCode.INSUFFICIENT_BALANCE);
        when(retryingWithdrawService.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY)).thenThrow(notRetried);

        // when & then
        assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
                .isSameAs(notRetried);
    }

    @Test
    @DisplayName("getStatus()는 계좌 검증 없이 재시도 서비스로 그대로 위임한다")
    void getStatus는_그대로_위임한다() {
        // given
        Long withdrawRequestId = 1L;

        // when
        sut.getStatus(withdrawRequestId);

        // then
        verify(retryingWithdrawService).getStatus(withdrawRequestId);
        verify(withdrawApplicationService, never()).validateBankAccount(org.mockito.ArgumentMatchers.any());
    }
}