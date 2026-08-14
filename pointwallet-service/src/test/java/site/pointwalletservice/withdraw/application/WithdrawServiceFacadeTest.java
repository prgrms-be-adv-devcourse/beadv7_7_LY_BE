package site.pointwalletservice.withdraw.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryException;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.domain.WithdrawStatus;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;
import site.pointwalletservice.withdraw.exception.WithdrawLockContentionException;

@DisplayName("WithdrawServiceFacade - 재시도 소진 시 예외 언래핑")
class WithdrawServiceFacadeTest {

    private final RetryingWithdrawService retryingWithdrawService = mock(RetryingWithdrawService.class);
    private final WithdrawServiceFacade sut = new WithdrawServiceFacade(retryingWithdrawService);

    private static final Long USER_ID = 1L;
    private static final Money AMOUNT = Money.of(100_000);

    @Test
    @DisplayName("성공하면 RetryingWithdrawService의 결과를 그대로 반환한다")
    void 성공하면_결과를_그대로_반환한다() {
        // given
        WithdrawRequestResult expected = new WithdrawRequestResult(
                1L, WithdrawStatus.SUCCESS, BigDecimal.valueOf(2_000), BigDecimal.valueOf(98_000));
        when(retryingWithdrawService.requestWithdraw(USER_ID, AMOUNT)).thenReturn(expected);

        // when
        WithdrawRequestResult result = sut.requestWithdraw(USER_ID, AMOUNT);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("재시도 소진으로 UndeclaredThrowableException(cause=RetryException(cause=WithdrawLockContentionException))이 " +
            "오면, 원래 도메인 예외까지 두 겹 풀어서 그대로 던진다")
    void 재시도소진시_예외를_풀어서_원래_도메인예외를_던진다() {
        // given
        WithdrawLockContentionException domainCause = new WithdrawLockContentionException();
        RetryException retryException = new RetryException("재시도 소진", domainCause);
        when(retryingWithdrawService.requestWithdraw(USER_ID, AMOUNT))
                .thenThrow(new UndeclaredThrowableException(retryException));

        // when & then
        assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT))
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
        when(retryingWithdrawService.requestWithdraw(USER_ID, AMOUNT)).thenThrow(notRetried);

        // when & then
        assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT))
                .isSameAs(notRetried);
    }

    @Test
    @DisplayName("getStatus()는 재시도 없이 그대로 위임한다")
    void getStatus는_그대로_위임한다() {
        // given
        Long withdrawRequestId = 1L;

        // when
        sut.getStatus(withdrawRequestId);

        // then
        org.mockito.Mockito.verify(retryingWithdrawService).getStatus(withdrawRequestId);
    }
}