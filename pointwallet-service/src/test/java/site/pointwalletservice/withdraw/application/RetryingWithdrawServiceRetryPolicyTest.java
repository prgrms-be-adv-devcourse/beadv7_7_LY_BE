package site.pointwalletservice.withdraw.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.withdraw.domain.Withdraw;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;
import site.pointwalletservice.withdraw.exception.WithdrawLockContentionException;

/**
 * RetryingHoldServiceRetryPolicyTest와 동일한 이유로, @Retryable(AOP 프록시)은 순수 Mockito
 * 단위테스트로 검증이 안 돼서 동일 정책의 RetryTemplate을 직접 구성해 검증한다.
 * <p>
 * 재시도 대상이 executeDeductionAndOutbox()로 바뀌었다 - 계좌 조회(validateBankAccount)는
 * WithdrawServiceFacade가 재시도 루프 진입 전에 1회만 부르므로 여기 대상이 아니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetryingWithdrawService의 재시도 정책 (executeDeductionAndOutbox()만 재시도)")
class RetryingWithdrawServiceRetryPolicyTest {

    @Mock
    private WithdrawApplicationService withdrawApplicationService;

    private RetryTemplate retryTemplate;

    private static final Long USER_ID = 1L;
    private static final Money AMOUNT = Money.of(100_000);
    private static final Money FEE_AMOUNT = Money.of(2_000);
    private static final Money NET_AMOUNT = Money.of(98_000);

    @BeforeEach
    void setUp() {
        // RetryingWithdrawService.requestWithdraw()의
        // @Retryable(includes=WithdrawLockContentionException.class, maxRetries=5, delay=50,
        // jitter=25, multiplier=2, maxDelay=800)와 동일한 정책(jitter는 타이밍에만 영향, 횟수 검증엔 무관).
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .includes(WithdrawLockContentionException.class)
                .maxRetries(5)
                .delay(Duration.ZERO)
                .build();
        retryTemplate = new RetryTemplate(retryPolicy);
    }

    private Withdraw stubWithdraw() {
        Withdraw withdraw = Withdraw.request(USER_ID, AMOUNT, FEE_AMOUNT, NET_AMOUNT);
        ReflectionTestUtils.setField(withdraw, "id", 1L);
        withdraw.complete();
        return withdraw;
    }

    @Test
    @DisplayName("락 경합으로 몇 번 실패하다가 성공하면, 그 시점까지 재시도하고 결과를 반환한다")
    void 경합후_성공하면_재시도끝에_결과를_반환한다() throws RetryException {
        // given: 2번은 경합 실패, 3번째에 성공
        Withdraw expected = stubWithdraw();
        when(withdrawApplicationService.executeDeductionAndOutbox(USER_ID, AMOUNT))
                .thenThrow(new WithdrawLockContentionException())
                .thenThrow(new WithdrawLockContentionException())
                .thenReturn(expected);

        // when
        Withdraw result = retryTemplate.execute(() ->
                withdrawApplicationService.executeDeductionAndOutbox(USER_ID, AMOUNT));

        // then
        assertThat(result).isEqualTo(expected);
        verify(withdrawApplicationService, times(3)).executeDeductionAndOutbox(USER_ID, AMOUNT);
    }

    @Test
    @DisplayName("maxRetries만큼 계속 실패하면 더 재시도하지 않고 RetryException(cause=마지막 예외)으로 감싸 던진다")
    void maxRetries까지_계속_실패하면_RetryException으로_감싸_던진다() {
        // given
        WithdrawLockContentionException lastFailure = new WithdrawLockContentionException();
        when(withdrawApplicationService.executeDeductionAndOutbox(USER_ID, AMOUNT))
                .thenThrow(new WithdrawLockContentionException())
                .thenThrow(new WithdrawLockContentionException())
                .thenThrow(new WithdrawLockContentionException())
                .thenThrow(new WithdrawLockContentionException())
                .thenThrow(new WithdrawLockContentionException())
                .thenThrow(lastFailure);

        // when & then: maxRetries=5 → 총 6번(초기 1 + 재시도 5) 시도
        assertThatThrownBy(() -> retryTemplate.execute(() ->
                withdrawApplicationService.executeDeductionAndOutbox(USER_ID, AMOUNT)))
                .isInstanceOf(RetryException.class)
                .cause().isSameAs(lastFailure);

        verify(withdrawApplicationService, times(6)).executeDeductionAndOutbox(USER_ID, AMOUNT);
    }

    @Test
    @DisplayName("경합과 무관한 예외(잔액부족 등)는 재시도 대상이 아니라 첫 시도에서 끝난다")
    void 경합이_아닌_예외는_재시도하지_않는다() {
        // given
        WithdrawException notRetryable = new WithdrawException(WithdrawErrorCode.INSUFFICIENT_BALANCE);
        when(withdrawApplicationService.executeDeductionAndOutbox(USER_ID, AMOUNT)).thenThrow(notRetryable);

        // when & then
        assertThatThrownBy(() -> retryTemplate.execute(() ->
                withdrawApplicationService.executeDeductionAndOutbox(USER_ID, AMOUNT)))
                .satisfiesAnyOf(
                        ex -> assertThat(ex).isSameAs(notRetryable),
                        ex -> assertThat(ex).isInstanceOf(RetryException.class).cause().isSameAs(notRetryable)
                );

        verify(withdrawApplicationService, times(1)).executeDeductionAndOutbox(USER_ID, AMOUNT);
    }
}