package site.pointwalletservice.hold.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.UndeclaredThrowableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryException;
import site.pointwalletservice.hold.exception.HoldErrorCode;
import site.pointwalletservice.hold.exception.HoldException;
import site.pointwalletservice.hold.exception.HoldLockContentionException;
import site.pointwalletservice.hold.exception.HoldRowLockContentionException;
import site.pointwalletservice.shared.Money;

@DisplayName("HoldServiceFacade - 재시도 소진 시 예외 언래핑")
class HoldServiceFacadeTest {

    private final RetryingHoldService retryingHoldService = mock(RetryingHoldService.class);
    private final HoldServiceFacade sut = new HoldServiceFacade(retryingHoldService);

    private static final Long AUCTION_ID = 5001L;
    private static final Long BIDDER_ID = 456L;
    private static final Money AMOUNT = Money.of(15_000);

    @Test
    @DisplayName("성공하면 RetryingHoldService의 결과를 그대로 반환한다")
    void 성공하면_결과를_그대로_반환한다() {
        // given
        HoldResult expected = new HoldResult(999L, null, Money.of(85_000));
        when(retryingHoldService.hold(AUCTION_ID, BIDDER_ID, AMOUNT)).thenReturn(expected);

        // when
        HoldResult result = sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("재시도 소진으로 UndeclaredThrowableException(cause=RetryException(cause=HoldLockContentionException))이 " +
            "오면, 원래 도메인 예외까지 두 겹 풀어서 그대로 던진다")
    void 재시도소진시_예외를_풀어서_원래_도메인예외를_던진다() {
        // given: RetryException은 checked exception이라 HoldService.hold()가 선언 안 한 채로
        // 프록시를 통과하면 실제로는 UndeclaredThrowableException으로 감싸져서 나온다 - 그 실제
        // 런타임 형태를 그대로 재현한다.
        HoldLockContentionException domainCause = new HoldLockContentionException();
        RetryException retryException = new RetryException("재시도 소진", domainCause);
        when(retryingHoldService.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                .thenThrow(new UndeclaredThrowableException(retryException));

        // when & then: 컨트롤러 입장에서는 이런 감싸기가 있다는 것 자체를 몰라도 됨 -
        // 평소와 똑같은 HoldException 계열만 받는다.
        assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                .isSameAs(domainCause)
                .isInstanceOf(HoldException.class)
                .extracting(e -> ((HoldException) e).getErrorCode())
                .isEqualTo(HoldErrorCode.LOCK_ACQUISITION_FAILED);
    }

    @Test
    @DisplayName("경합과 무관한 예외(잔액부족 등)는 재시도 자체가 안 걸려서 UndeclaredThrowableException 없이 그대로 전파된다")
    void 경합과_무관한_예외는_그대로_전파된다() {
        // given
        HoldException notRetried = new HoldException(HoldErrorCode.INSUFFICIENT_BALANCE);
        when(retryingHoldService.hold(AUCTION_ID, BIDDER_ID, AMOUNT)).thenThrow(notRetried);

        // when & then
        assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                .isSameAs(notRetried);
    }

    @Test
    @DisplayName("Hold 행 락 경합(HoldRowLockContentionException)도 재시도 대상이 아니라 UndeclaredThrowableException 없이 그대로 전파된다")
    void Hold행_락_경합은_재시도_대상이_아니라_그대로_전파된다() {
        // given: auction-service가 즉시 알아야 하는 신호라 RetryingHoldService의 @Retryable(includes=...)
        // 대상에서 제외돼 있다 - 그래서 재시도가 아예 안 걸리고, RetryException/UndeclaredThrowableException으로
        // 감싸지지도 않고 첫 시도에서 바로 전파돼야 한다.
        HoldRowLockContentionException notRetried = new HoldRowLockContentionException();
        when(retryingHoldService.hold(AUCTION_ID, BIDDER_ID, AMOUNT)).thenThrow(notRetried);

        // when & then
        assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                .isSameAs(notRetried);
    }

    @Test
    @DisplayName("release()/consume()은 재시도 없이 그대로 위임한다")
    void release_consume은_그대로_위임한다() {
        // when
        sut.release(AUCTION_ID);
        sut.consume(AUCTION_ID);

        // then
        verify(retryingHoldService).release(AUCTION_ID);
        verify(retryingHoldService).consume(AUCTION_ID);
    }
}