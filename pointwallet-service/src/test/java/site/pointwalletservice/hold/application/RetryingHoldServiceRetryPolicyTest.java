package site.pointwalletservice.hold.application;
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
import site.pointwalletservice.hold.exception.HoldErrorCode;
import site.pointwalletservice.hold.exception.HoldException;
import site.pointwalletservice.hold.exception.HoldLockContentionException;
import site.pointwalletservice.hold.exception.HoldRowLockContentionException;
import site.pointwalletservice.shared.Money;

/**
 * RetryingHoldService.hold()의 @Retryable은 스프링 프레임워크 7의 core 내장 기능(AOP 프록시)이라
 * 순수 Mockito 단위테스트로는 재시도 자체를 검증할 수 없다(프록시 없이 순수 객체를 호출하는 것이므로
 * 어노테이션이 무시된다). 그래서 어노테이션에 명시한 것과 동일한 정책으로 만든 RetryTemplate을
 * 직접 구성해서, "이 정책대로 돌리면 실제로 N번째 성공 시 멈추고, 계속 실패하면 maxRetries에서
 * 포기하는지"를 검증한다.
 * <p>
 * 주의: RetryTemplate.execute()는 재시도가 소진되면 원래 예외를 그대로 던지지 않고 RetryException으로
 * 감싸서 던진다(cause에 마지막 예외가 담김) - HoldServiceFacade가 이걸 다시 풀어주는 부분과 같이
 * 봐야 한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetryingHoldService의 재시도 정책 (hold() 자체를 처음부터 재시도)")
class RetryingHoldServiceRetryPolicyTest {

    @Mock
    private HoldApplicationService holdApplicationService;

    private RetryTemplate retryTemplate;

    private static final Long AUCTION_ID = 5001L;
    private static final Long BIDDER_ID = 456L;
    private static final Money AMOUNT = Money.of(15_000);

    @BeforeEach
    void setUp() {
        // RetryingHoldService.hold()의 @Retryable(includes=HoldLockContentionException.class,
        // maxRetries=5, delay=50, multiplier=2, maxDelay=800)와 동일한 정책. 테스트에서는 delay를
        // 0으로 둬서 빠르게 돈다(정책의 "몇 번 재시도하는가"만 검증).
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .includes(HoldLockContentionException.class)
                .maxRetries(5)
                .delay(Duration.ZERO)
                .build();
        retryTemplate = new RetryTemplate(retryPolicy);
    }

    @Test
    @DisplayName("경합으로 몇 번 실패하다가 성공하면, 그 시점까지 재시도하고 결과를 반환한다")
    void 경합후_성공하면_재시도끝에_결과를_반환한다() throws RetryException {
        // given: 2번은 경합 실패, 3번째에 성공
        HoldResult expected = new HoldResult(999L, null, Money.of(85_000));
        when(holdApplicationService.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                .thenThrow(new HoldLockContentionException())
                .thenThrow(new HoldLockContentionException())
                .thenReturn(expected);

        // when
        HoldResult result = retryTemplate.execute(() ->
                holdApplicationService.hold(AUCTION_ID, BIDDER_ID, AMOUNT));

        // then
        assertThat(result).isEqualTo(expected);
        verify(holdApplicationService, times(3)).hold(AUCTION_ID, BIDDER_ID, AMOUNT);
    }

    @Test
    @DisplayName("maxRetries만큼 계속 실패하면 더 재시도하지 않고 RetryException(cause=마지막 예외)으로 감싸 던진다")
    void maxRetries까지_계속_실패하면_RetryException으로_감싸_던진다() {
        // given
        HoldLockContentionException lastFailure = new HoldLockContentionException();
        when(holdApplicationService.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                .thenThrow(new HoldLockContentionException())
                .thenThrow(new HoldLockContentionException())
                .thenThrow(new HoldLockContentionException())
                .thenThrow(new HoldLockContentionException())
                .thenThrow(new HoldLockContentionException())
                .thenThrow(lastFailure);

        // when & then: maxRetries=5 → 총 6번(초기 1 + 재시도 5) 시도
        assertThatThrownBy(() -> retryTemplate.execute(() ->
                holdApplicationService.hold(AUCTION_ID, BIDDER_ID, AMOUNT)))
                .isInstanceOf(RetryException.class)
                .cause().isSameAs(lastFailure);

        verify(holdApplicationService, times(6)).hold(AUCTION_ID, BIDDER_ID, AMOUNT);
    }

    @Test
    @DisplayName("경합과 무관한 예외(잔액부족 등)는 재시도 대상이 아니라 첫 시도에서 끝난다")
    void 경합이_아닌_예외는_재시도하지_않는다() {
        // given
        HoldException notRetryable = new HoldException(HoldErrorCode.INSUFFICIENT_BALANCE);
        when(holdApplicationService.hold(AUCTION_ID, BIDDER_ID, AMOUNT)).thenThrow(notRetryable);

        // when & then: RetryPolicy에 includes(HoldLockContentionException) 외 타입이라 재시도가 안 걸린다.
        // RetryTemplate이 이걸 원본 그대로 던지는지 RetryException으로 한 번 감싸는지는 실행해보기 전엔
        // 단정할 수 없어서(문서상 명시 안 됨) 둘 다 허용하되, 핵심은 "결국 notRetryable로 귀결되고
        // 딱 한 번만 시도됐는지"다 - 둘 중 어느 쪽이든 HoldServiceFacade가 cause를 풀어주므로
        // 실제 응답 결과는 동일하다.
        assertThatThrownBy(() -> retryTemplate.execute(() ->
                holdApplicationService.hold(AUCTION_ID, BIDDER_ID, AMOUNT)))
                .satisfiesAnyOf(
                        ex -> assertThat(ex).isSameAs(notRetryable),
                        ex -> assertThat(ex).isInstanceOf(RetryException.class).cause().isSameAs(notRetryable)
                );

        verify(holdApplicationService, times(1)).hold(AUCTION_ID, BIDDER_ID, AMOUNT);
    }

    @Test
    @DisplayName("Hold 행 락 경합(HoldRowLockContentionException)도 재시도 대상이 아니라 첫 시도에서 끝난다")
    void Hold행_락_경합은_재시도하지_않는다() {
        // given: auction-service가 즉시 알아야 하는 신호라 일부러 includes에서 뺐다
        // (HoldRowLockContentionException 클래스 주석 참고).
        HoldRowLockContentionException notRetryable = new HoldRowLockContentionException();
        when(holdApplicationService.hold(AUCTION_ID, BIDDER_ID, AMOUNT)).thenThrow(notRetryable);

        // when & then
        assertThatThrownBy(() -> retryTemplate.execute(() ->
                holdApplicationService.hold(AUCTION_ID, BIDDER_ID, AMOUNT)))
                .satisfiesAnyOf(
                        ex -> assertThat(ex).isSameAs(notRetryable),
                        ex -> assertThat(ex).isInstanceOf(RetryException.class).cause().isSameAs(notRetryable)
                );

        verify(holdApplicationService, times(1)).hold(AUCTION_ID, BIDDER_ID, AMOUNT);
    }
}

/**
 * RetryingHoldService.rollback()의 재시도 정책 — hold()와 같은 파라미터(maxRetries=5, delay=50,
 * jitter=25, multiplier=2, maxDelay=800)지만 includes 대상이 다르다: HoldRowLockContentionException도
 * 포함한다(hold()는 뺐음 - RetryingHoldService 클래스 주석 및 rollback() 메서드 주석 참고).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetryingHoldService의 재시도 정책 (rollback() - hold()와 달리 HoldRowLockContentionException도 재시도)")
class RetryingHoldServiceRollbackRetryPolicyTest {

    @Mock
    private HoldApplicationService holdApplicationService;

    private RetryTemplate retryTemplate;

    private static final Long HOLD_ID = 1L;
    private static final Long AUCTION_ID = 5001L;
    private static final Long BIDDER_ID = 456L;
    private static final Money AMOUNT = Money.of(15_000);

    @BeforeEach
    void setUp() {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .includes(HoldLockContentionException.class, HoldRowLockContentionException.class)
                .maxRetries(5)
                .delay(Duration.ZERO)
                .build();
        retryTemplate = new RetryTemplate(retryPolicy);
    }

    @Test
    @DisplayName("지갑 락 경합(HoldLockContentionException)도 재시도 대상이다")
    void 지갑락_경합도_재시도한다() throws RetryException {
        // given
        org.mockito.Mockito.doThrow(new HoldLockContentionException())
                .doThrow(new HoldLockContentionException())
                .doNothing()
                .when(holdApplicationService).rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);

        // when
        retryTemplate.execute(() -> {
            holdApplicationService.rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);
            return null;
        });

        // then
        verify(holdApplicationService, times(3)).rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);
    }

    @Test
    @DisplayName("Hold 행 락 경합(HoldRowLockContentionException)도 hold()와 달리 재시도 대상이다")
    void Hold행_락_경합도_재시도한다() throws RetryException {
        // given
        org.mockito.Mockito.doThrow(new HoldRowLockContentionException())
                .doThrow(new HoldRowLockContentionException())
                .doNothing()
                .when(holdApplicationService).rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);

        // when
        retryTemplate.execute(() -> {
            holdApplicationService.rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);
            return null;
        });

        // then
        verify(holdApplicationService, times(3)).rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);
    }

    @Test
    @DisplayName("원장 불일치(HOLD_ALREADY_FINALIZED/HOLD_MISMATCH)는 재시도해도 결과가 안 바뀌므로 대상에서 제외되어 첫 시도에서 끝난다")
    void 원장불일치는_재시도하지_않는다() {
        // given
        HoldException notRetryable = new HoldException(HoldErrorCode.HOLD_ALREADY_FINALIZED);
        org.mockito.Mockito.doThrow(notRetryable)
                .when(holdApplicationService).rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);

        // when & then
        assertThatThrownBy(() -> retryTemplate.execute(() -> {
            holdApplicationService.rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);
            return null;
        })).satisfiesAnyOf(
                ex -> assertThat(ex).isSameAs(notRetryable),
                ex -> assertThat(ex).isInstanceOf(RetryException.class).cause().isSameAs(notRetryable)
        );

        verify(holdApplicationService, times(1)).rollback(HOLD_ID, AUCTION_ID, BIDDER_ID, AMOUNT);
    }
}